import asyncio
import json
import uuid

import httpx

from app.audio.audio_buffer import UtteranceSegmenter
from app.audio.audio_router import listener_should_hear, other_language, translated_track_name
from app.audio.pipeline import GenerationGuard, TranslationPipeline
from app.config.settings import settings
from app.providers.factory import ProviderNotConfigured, build_pipeline


async def run_call(ctx) -> None:
    """Join a LiveKit room and translate each speaker into the other participant's language."""
    await ctx.connect()
    room = ctx.room
    metadata = _metadata(room)
    call_id = metadata.get("callId")
    participants = {
        identity: value.get("language")
        for identity, value in metadata.get("participants", {}).items()
        if isinstance(value, dict) and value.get("language")
    }
    profiles = metadata.get("participants", {})
    try:
        pipeline = build_pipeline()
    except ProviderNotConfigured as exc:
        print(f"translator not configured: {exc}")
        return

    tasks: set[asyncio.Task] = set()
    closed = asyncio.Event()

    @room.on("disconnected")
    def on_disconnected(*_args):
        closed.set()

    @room.on("track_subscribed")
    def on_track(track, publication, participant):
        name = getattr(publication, "name", "") or ""
        if name.startswith("tx:") or participant.identity not in participants:
            return
        task = asyncio.create_task(
            _follow_speaker(room, pipeline, call_id, participant, track, publication, participants, profiles)
        )
        tasks.add(task)
        task.add_done_callback(tasks.discard)

    await closed.wait()


async def _follow_speaker(room, pipeline: TranslationPipeline, call_id, participant, track, publication, participants, profiles) -> None:
    from livekit import rtc

    speaker_id = participant.identity
    source = participants[speaker_id]
    target = other_language(participants, speaker_id)
    if target is None:
        return
    profile = profiles.get(speaker_id, {})
    terms = [profile.get("displayName", "")]
    for other in profiles.values():
        if isinstance(other, dict):
            terms.append(other.get("displayName", ""))
    guard = GenerationGuard()
    segmenter = UtteranceSegmenter(max_seconds=settings.max_utterance_seconds)
    stream = rtc.AudioStream.from_track(track=track, sample_rate=16000, num_channels=1)
    source_out, translated_publication = await _publish_translation_track(room, target)
    await _set_subscription(room, publication, speaker_id, subscribe_speaker=True)
    await _set_subscription(room, translated_publication, speaker_id, subscribe_speaker=False)
    async for event in stream:
        frame = getattr(event, "frame", event)
        pcm = bytes(frame.data)
        for utterance in segmenter.feed(pcm):
            token = guard.bump()
            await _translate_and_publish(
                room,
                pipeline,
                source_out,
                call_id,
                speaker_id,
                source,
                target,
                profile,
                terms,
                utterance,
                guard,
                token,
            )


async def _translate_and_publish(
    room,
    pipeline,
    source_out,
    call_id,
    speaker_id,
    source,
    target,
    profile,
    terms,
    utterance,
    guard,
    token,
) -> None:
    from livekit import rtc

    utterance_id = str(uuid.uuid4())
    result = await pipeline.process_utterance(
        pcm=utterance,
        sample_rate=16000,
        speaker_id=speaker_id,
        source_language=source,
        target_language=target,
        voice=profile.get("voice") or None,
        gender=profile.get("voiceGender"),
        speed=float(profile.get("translationSpeed") or 1.0),
        protected_terms=terms,
        generation=guard,
        generation_token=token,
        utterance_id=utterance_id,
    )
    await _report(call_id, result, speaker_id, source, target)
    payload = json.dumps(
        {
            "callId": call_id,
            "speakerId": speaker_id,
            "sourceLanguage": source,
            "targetLanguage": target,
            "originalText": result.original_text,
            "translatedText": result.translated_text,
            "status": result.status,
            "partial": False,
            "utteranceId": utterance_id,
            "warnings": result.warnings,
        }
    ).encode()
    await room.local_participant.publish_data(payload, reliable=True, topic="omnicall.subtitle")
    if result.status != "TRANSLATED" or not result.audio or source_out is None:
        return
    if not guard.current(token):
        return
    pcm, rate = result.audio, result.sample_rate or 24000
    frame_samples = rate // 50
    for offset in range(0, len(pcm), frame_samples * 2):
        if not guard.current(token):
            return
        chunk = pcm[offset : offset + frame_samples * 2]
        if len(chunk) < 2:
            continue
        samples = len(chunk) // 2
        frame = rtc.AudioFrame(data=chunk, sample_rate=rate, num_channels=1, samples_per_channel=samples)
        await source_out.capture_frame(frame)


async def _publish_translation_track(room, target_language: str):
    from livekit import rtc

    name = translated_track_name(target_language)
    source = rtc.AudioSource(24000, 1)
    track = rtc.LocalAudioTrack.create_audio_track(name, source)
    publication = await room.local_participant.publish_track(track, rtc.TrackPublishOptions())
    return source, publication


async def _set_subscription(room, publication, speaker_id: str, subscribe_speaker: bool) -> None:
    """Keep original audio with the translator and give translated audio to the listener only."""
    from livekit import api

    sid = getattr(publication, "sid", None)
    if not sid:
        return
    lk = api.LiveKitAPI(settings.livekit_url, settings.livekit_api_key, settings.livekit_api_secret)
    try:
        for participant in room.remote_participants.values():
            hears = participant.identity == speaker_id if subscribe_speaker else participant.identity != speaker_id
            if not subscribe_speaker:
                hears = hears and listener_should_hear(getattr(publication, "name", "") or "", _language_of(participant), True)
            await lk.room.update_subscriptions(
                api.UpdateSubscriptionsRequest(
                    room=room.name,
                    identity=participant.identity,
                    track_sids=[sid],
                    subscribe=hears,
                )
            )
    finally:
        await lk.aclose()


def _language_of(participant) -> str:
    raw = participant.metadata or "{}"
    try:
        return json.loads(raw).get("language", "")
    except json.JSONDecodeError:
        return ""


def _metadata(room) -> dict:
    raw = room.metadata or "{}"
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {}


async def _report(call_id, result, speaker_id, source, target) -> None:
    if not call_id or result.status in {"NO_SPEECH", "DUPLICATE", "CANCELLED"}:
        return
    body = {
        "speakerId": speaker_id,
        "sourceLanguage": source,
        "targetLanguage": target,
        "originalText": result.original_text or "",
        "translatedText": result.translated_text,
        "status": result.status,
        "partial": False,
        "utteranceId": result.utterance_id,
    }
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            await client.post(
                f"{settings.backend_url}/api/internal/calls/{call_id}/translations",
                json=body,
                headers={"X-OmniCall-Internal-Token": settings.internal_api_token},
            )
    except httpx.HTTPError:
        return
