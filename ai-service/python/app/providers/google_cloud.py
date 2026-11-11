import asyncio

from app.audio.audio_processor import strip_wav_header
from app.config.languages import voice_for
from app.providers.base import SynthesizedAudio, Transcript


class GoogleSpeechProvider:
    async def transcribe_utterance(self, pcm: bytes, sample_rate: int, language: str) -> Transcript:
        text = await asyncio.to_thread(self._recognize, pcm, sample_rate, language)
        return Transcript(text=text, language=language, partial=False)

    def _recognize(self, pcm: bytes, sample_rate: int, language: str) -> str:
        from google.cloud import speech

        client = speech.SpeechClient()
        config = speech.RecognitionConfig(
            encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
            sample_rate_hertz=sample_rate,
            language_code=language,
            enable_automatic_punctuation=True,
        )
        response = client.recognize(config=config, audio=speech.RecognitionAudio(content=pcm))
        parts = [result.alternatives[0].transcript for result in response.results if result.alternatives]
        return " ".join(part.strip() for part in parts if part.strip())


class GoogleTranslationProvider:
    def __init__(self, project_id: str) -> None:
        self.project_id = project_id

    async def translate(self, text: str, source_language: str, target_language: str, context: list[str]) -> str:
        del context
        return await asyncio.to_thread(self._translate, text, source_language, target_language)

    def _translate(self, text: str, source_language: str, target_language: str) -> str:
        from google.cloud import translate

        from app.config.languages import LANGUAGES

        if not self.project_id:
            raise RuntimeError("GOOGLE_CLOUD_PROJECT is not set")
        client = translate.TranslationServiceClient()
        response = client.translate_text(
            request={
                "parent": f"projects/{self.project_id}/locations/global",
                "contents": [text],
                "mime_type": "text/plain",
                "source_language_code": LANGUAGES[source_language].translation_code,
                "target_language_code": LANGUAGES[target_language].translation_code,
            }
        )
        return response.translations[0].translated_text


class GoogleTextToSpeechProvider:
    async def synthesize(self, text: str, language: str, voice: str | None, gender: str | None, speed: float) -> SynthesizedAudio:
        pcm, sample_rate = await asyncio.to_thread(self._synthesize, text, language, voice, gender, speed)
        return SynthesizedAudio(pcm=pcm, sample_rate=sample_rate)

    def _synthesize(self, text: str, language: str, voice: str | None, gender: str | None, speed: float) -> tuple[bytes, int]:
        from google.cloud import texttospeech

        client = texttospeech.TextToSpeechClient()
        selected = voice_for(language, gender, voice)
        voice_params = texttospeech.VoiceSelectionParams(
            language_code=language,
            name=selected or "",
            ssml_gender=_gender(texttospeech, gender),
        )
        if not selected:
            voice_params = texttospeech.VoiceSelectionParams(
                language_code=language,
                ssml_gender=_gender(texttospeech, gender),
            )
        audio_config = texttospeech.AudioConfig(
            audio_encoding=texttospeech.AudioEncoding.LINEAR16,
            sample_rate_hertz=24000,
            speaking_rate=speed,
        )
        response = client.synthesize_speech(
            input=texttospeech.SynthesisInput(text=text),
            voice=voice_params,
            audio_config=audio_config,
        )
        return strip_wav_header(response.audio_content)


def _gender(texttospeech, gender: str | None):
    name = (gender or "FEMALE").upper()
    mapping = {
        "MALE": texttospeech.SsmlVoiceGender.MALE,
        "FEMALE": texttospeech.SsmlVoiceGender.FEMALE,
        "NEUTRAL": texttospeech.SsmlVoiceGender.NEUTRAL,
    }
    return mapping.get(name, texttospeech.SsmlVoiceGender.FEMALE)
