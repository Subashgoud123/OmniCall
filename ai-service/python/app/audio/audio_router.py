def translated_track_name(target_language: str) -> str:
    return f"tx:{target_language}"


def listener_should_hear(track_name: str, listener_language: str, original_audio: bool) -> bool:
    """Translated audio is delivered by language. Original audio stays off unless requested."""
    if track_name.startswith("tx:"):
        return track_name.removeprefix("tx:") == listener_language
    return original_audio


def other_language(participants: dict[str, str], speaker_id: str) -> str | None:
    speaker = participants.get(speaker_id)
    others = [language for identity, language in participants.items() if identity != speaker_id]
    if speaker is None or not others:
        return None
    return others[0]
