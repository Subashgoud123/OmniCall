from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    livekit_url: str = "ws://localhost:7880"
    livekit_api_key: str = "devkey"
    livekit_api_secret: str = "secret"
    agent_name: str = "omnicall-translator"
    backend_url: str = "http://localhost:8080"
    internal_api_token: str = "dev-internal-token"
    google_cloud_project: str = ""
    ai_provider: str = "google"
    sample_rate: int = 16000
    tts_sample_rate: int = 24000
    translation_timeout_seconds: float = 8.0
    speech_timeout_seconds: float = 20.0
    tts_timeout_seconds: float = 12.0
    max_utterance_seconds: float = 15.0


settings = Settings()
