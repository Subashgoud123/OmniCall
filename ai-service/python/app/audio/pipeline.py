import time
from dataclasses import dataclass

from app.providers.base import SpeechRecognitionProvider, TextToSpeechProvider
from app.translation.translator import Translator


@dataclass
class PipelineResult:
    original_text: str | None
    translated_text: str | None
    status: str
    warnings: list[str]
    audio: bytes | None
    sample_rate: int | None
    utterance_id: str


class GenerationGuard:
    def __init__(self) -> None:
        self.token = 0

    def bump(self) -> int:
        self.token += 1
        return self.token

    def current(self, token: int) -> bool:
        return token == self.token


class DuplicateFilter:
    def __init__(self, window_seconds: float = 3.0) -> None:
        self.window_seconds = window_seconds
        self._seen: dict[str, tuple[str, float]] = {}

    def is_duplicate(self, speaker_id: str, text: str) -> bool:
        key = " ".join(text.lower().split())
        now = time.monotonic()
        previous = self._seen.get(speaker_id)
        self._seen[speaker_id] = (key, now)
        return previous is not None and previous[0] == key and now - previous[1] < self.window_seconds


class TranslationPipeline:
    def __init__(
        self,
        speech: SpeechRecognitionProvider,
        translator: Translator,
        tts: TextToSpeechProvider,
    ) -> None:
        self.speech = speech
        self.translator = translator
        self.tts = tts
        self.duplicates = DuplicateFilter()

    async def process_utterance(
        self,
        pcm: bytes,
        sample_rate: int,
        speaker_id: str,
        source_language: str,
        target_language: str,
        voice: str | None,
        gender: str | None,
        speed: float,
        protected_terms: list[str],
        generation: GenerationGuard,
        generation_token: int,
        utterance_id: str,
    ) -> PipelineResult:
        if not generation.current(generation_token):
            return self._cancelled(utterance_id)
        transcript = await self.speech.transcribe_utterance(pcm, sample_rate, source_language)
        original = (transcript.text or "").strip()
        if not generation.current(generation_token):
            return self._cancelled(utterance_id)
        if not original:
            return PipelineResult(None, None, "NO_SPEECH", [], None, None, utterance_id)
        if self.duplicates.is_duplicate(speaker_id, original):
            return PipelineResult(original, None, "DUPLICATE", [], None, None, utterance_id)
        translated = await self.translator.translate(original, source_language, target_language, protected_terms)
        if not generation.current(generation_token):
            return self._cancelled(utterance_id)
        if not translated.succeeded or not translated.text:
            return PipelineResult(original, None, "TRANSLATION_FAILED", translated.warnings, None, None, utterance_id)
        audio = await self.tts.synthesize(translated.text, target_language, voice, gender, speed)
        if not generation.current(generation_token):
            return self._cancelled(utterance_id)
        return PipelineResult(
            original,
            translated.text,
            "TRANSLATED",
            translated.warnings,
            audio.pcm,
            audio.sample_rate,
            utterance_id,
        )

    def _cancelled(self, utterance_id: str) -> PipelineResult:
        return PipelineResult(None, None, "CANCELLED", [], None, None, utterance_id)
