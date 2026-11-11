from app.audio.audio_router import listener_should_hear, other_language
from app.audio.pipeline import DuplicateFilter, GenerationGuard, TranslationPipeline
from app.providers.base import SynthesizedAudio, Transcript, TranslationResult
from app.speech.vad import VoiceActivityDetector
from app.translation.context_manager import ContextManager
from app.translation.protector import protect, restore
from app.translation.translator import Translator
import pytest


class ScriptSpeech:
    async def transcribe_utterance(self, pcm, sample_rate, language):
        return Transcript(text="house 42", language=language)


class EchoTranslation:
    async def translate(self, text, source_language, target_language, context):
        return f"{text} [{target_language}]"


class FailingTranslation:
    async def translate(self, text, source_language, target_language, context):
        raise RuntimeError("provider down")


class SilentSpeech:
    async def synthesize(self, text, language, voice, gender, speed):
        return SynthesizedAudio(pcm=b"\x00\x00" * 10, sample_rate=24000)


def test_numbers_are_protected_and_restored():
    protected, values = protect("Meet at 5:30 near 42", [])
    assert "5:30" not in protected
    assert "42" not in protected
    restored, missing = restore(protected.replace("Meet", "కలవండి"), values)
    assert missing == []
    assert "5:30" in restored
    assert "42" in restored


def test_missing_token_is_reported():
    _, missing = restore("translated without tokens", ["42"])
    assert missing == ["42"]


def test_context_window_drops_old_lines():
    context = ContextManager(limit=2)
    context.remember("a", "A")
    context.remember("b", "B")
    context.remember("c", "C")
    assert len(context.recent()) == 2
    assert context.recent()[0].startswith("b")


def test_failed_translation_does_not_copy_the_original():
    async def run():
        translator = Translator(FailingTranslation(), ContextManager())
        result = await translator.translate("hello", "hi-IN", "te-IN", [])
        assert result.succeeded is False
        assert result.text is None

    import asyncio
    asyncio.run(run())


def test_vad_ignores_silence_and_hears_a_tone():
    vad = VoiceActivityDetector(threshold=100)
    assert vad.is_speech(b"\x00\x00" * 320) is False
    loud = (b"\xff\x0f" * 320)
    assert vad.is_speech(loud) is True


def test_router_keeps_original_audio_off_and_selects_the_listener_language():
    assert listener_should_hear("microphone", "te-IN", False) is False
    assert listener_should_hear("tx:te-IN", "te-IN", False) is True
    assert listener_should_hear("tx:hi-IN", "te-IN", False) is False
    assert other_language({"a": "hi-IN", "b": "te-IN"}, "a") == "te-IN"


def test_duplicate_and_interruption():
    duplicates = DuplicateFilter(window_seconds=5)
    assert duplicates.is_duplicate("a", "hello") is False
    assert duplicates.is_duplicate("a", "hello") is True
    guard = GenerationGuard()
    first = guard.bump()
    second = guard.bump()
    assert guard.current(first) is False
    assert guard.current(second) is True


def test_pipeline_reports_translation_failure_without_audio():
    async def run():
        pipeline = TranslationPipeline(ScriptSpeech(), Translator(FailingTranslation(), ContextManager()), SilentSpeech())
        guard = GenerationGuard()
        token = guard.bump()
        result = await pipeline.process_utterance(
            b"\x01\x00" * 1600,
            16000,
            "speaker",
            "hi-IN",
            "te-IN",
            None,
            "FEMALE",
            1.0,
            [],
            guard,
            token,
            "u1",
        )
        assert result.status == "TRANSLATION_FAILED"
        assert result.original_text == "house 42"
        assert result.translated_text is None
        assert result.audio is None

    import asyncio
    asyncio.run(run())
