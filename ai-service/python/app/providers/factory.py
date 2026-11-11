from app.audio.pipeline import TranslationPipeline
from app.config.settings import settings
from app.providers.speech_provider import SpeechRecognizer
from app.providers.translation_provider import TranslationClient
from app.providers.tts_provider import SpeechSynthesizer
from app.translation.context_manager import ContextManager
from app.translation.translator import Translator


class ProviderNotConfigured(RuntimeError):
    pass


def build_pipeline() -> TranslationPipeline:
    if settings.ai_provider != "google":
        raise ProviderNotConfigured(f"Unknown AI provider {settings.ai_provider}")
    if not settings.google_cloud_project:
        raise ProviderNotConfigured("Set GOOGLE_CLOUD_PROJECT and GOOGLE_APPLICATION_CREDENTIALS")
    return TranslationPipeline(
        speech=SpeechRecognizer(),
        translator=Translator(TranslationClient(settings.google_cloud_project), ContextManager()),
        tts=SpeechSynthesizer(),
    )
