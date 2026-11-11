from dataclasses import dataclass


@dataclass(frozen=True)
class Language:
    code: str
    translation_code: str
    english_name: str
    speech: bool
    translation: bool
    tts: bool
    enabled: bool


# Adding a language is a registry entry. Hindi and Telugu are the enabled pair.
LANGUAGES: dict[str, Language] = {
    "hi-IN": Language("hi-IN", "hi", "Hindi", True, True, True, True),
    "te-IN": Language("te-IN", "te", "Telugu", True, True, True, True),
    "en-IN": Language("en-IN", "en", "English", True, True, True, False),
    "ta-IN": Language("ta-IN", "ta", "Tamil", True, True, True, False),
    "kn-IN": Language("kn-IN", "kn", "Kannada", True, True, True, False),
    "ml-IN": Language("ml-IN", "ml", "Malayalam", True, True, True, False),
    "mr-IN": Language("mr-IN", "mr", "Marathi", True, True, True, False),
    "bn-IN": Language("bn-IN", "bn", "Bengali", True, True, True, False),
    "gu-IN": Language("gu-IN", "gu", "Gujarati", True, True, True, False),
    "pa-IN": Language("pa-IN", "pa", "Punjabi", True, True, True, False),
}

FEMALE_VOICES = {
    "hi-IN": "hi-IN-Neural2-A",
    "te-IN": "te-IN-Standard-A",
    "en-IN": "en-IN-Neural2-A",
}

MALE_VOICES = {
    "hi-IN": "hi-IN-Neural2-B",
    "te-IN": "te-IN-Standard-B",
    "en-IN": "en-IN-Neural2-B",
}


def require_enabled(code: str) -> Language:
    language = LANGUAGES.get(code)
    if language is None or not language.enabled:
        raise ValueError(f"Language {code} is not enabled")
    return language


def voice_for(language: str, gender: str | None, explicit: str | None) -> str | None:
    if explicit:
        return explicit
    table = MALE_VOICES if (gender or "").upper() == "MALE" else FEMALE_VOICES
    return table.get(language)
