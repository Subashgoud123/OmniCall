from app.providers.base import TranslationProvider, TranslationResult
from app.translation.context_manager import ContextManager
from app.translation.protector import protect, restore


class Translator:
    def __init__(self, provider: TranslationProvider, context: ContextManager) -> None:
        self.provider = provider
        self.context = context

    async def translate(
        self,
        text: str,
        source_language: str,
        target_language: str,
        protected_terms: list[str],
    ) -> TranslationResult:
        if source_language == target_language:
            return TranslationResult(text=text, succeeded=True, warnings=[])
        cleaned = " ".join(text.split())
        if not cleaned:
            return TranslationResult(text=None, succeeded=False, warnings=["empty"])
        protected, replacements = protect(cleaned, protected_terms)
        try:
            raw = await self.provider.translate(protected, source_language, target_language, self.context.recent())
        except Exception:
            return TranslationResult(text=None, succeeded=False, warnings=["provider_failed"])
        if raw is None or not raw.strip():
            return TranslationResult(text=None, succeeded=False, warnings=["empty_translation"])
        restored, missing = restore(raw.strip(), replacements)
        warnings = ["token_not_preserved"] if missing else []
        self.context.remember(cleaned, restored)
        return TranslationResult(text=restored, succeeded=True, warnings=warnings)
