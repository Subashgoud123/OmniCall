from dataclasses import dataclass


@dataclass
class Transcript:
    text: str
    language: str
    partial: bool = False


@dataclass
class TranslationResult:
    text: str | None
    succeeded: bool
    warnings: list[str]


@dataclass
class SynthesizedAudio:
    pcm: bytes
    sample_rate: int


class SpeechRecognitionProvider:
    async def transcribe_utterance(self, pcm: bytes, sample_rate: int, language: str) -> Transcript:
        raise NotImplementedError


class TranslationProvider:
    async def translate(self, text: str, source_language: str, target_language: str, context: list[str]) -> str:
        raise NotImplementedError


class TextToSpeechProvider:
    async def synthesize(self, text: str, language: str, voice: str | None, gender: str | None, speed: float) -> SynthesizedAudio:
        raise NotImplementedError
