import base64

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

from app.audio.audio_processor import read_wav, write_wav
from app.audio.pipeline import GenerationGuard
from app.providers.factory import ProviderNotConfigured, build_pipeline

app = FastAPI(title="OmniCall AI", version="0.1.0")


class TextRequest(BaseModel):
    text: str
    source_language: str
    target_language: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "omnicall-ai"}


@app.get("/ready")
def ready() -> dict[str, str]:
    try:
        build_pipeline()
    except ProviderNotConfigured as exc:
        return {"status": "degraded", "detail": str(exc)}
    return {"status": "ok"}


@app.post("/api/translation/text")
async def translate_text(request: TextRequest) -> dict:
    pipeline = _pipeline()
    result = await pipeline.translator.translate(request.text, request.source_language, request.target_language, [])
    if not result.succeeded:
        raise HTTPException(status_code=502, detail={"code": "TRANSLATION_FAILED", "warnings": result.warnings})
    return {"originalText": request.text, "translatedText": result.text, "warnings": result.warnings, "status": "TRANSLATED"}


@app.post("/api/translation/speech")
async def translate_speech(
    source_language: str = Form(...),
    target_language: str = Form(...),
    file: UploadFile = File(...),
) -> dict:
    """Standalone speech test. Live calls use the LiveKit worker, not this endpoint."""
    pcm, sample_rate = read_wav(await file.read())
    pipeline = _pipeline()
    guard = GenerationGuard()
    token = guard.bump()
    result = await pipeline.process_utterance(
        pcm=pcm,
        sample_rate=sample_rate,
        speaker_id="file",
        source_language=source_language,
        target_language=target_language,
        voice=None,
        gender="FEMALE",
        speed=1.0,
        protected_terms=[],
        generation=guard,
        generation_token=token,
        utterance_id="file",
    )
    if result.status == "TRANSLATION_FAILED":
        return {
            "status": result.status,
            "originalText": result.original_text,
            "translatedText": None,
            "warnings": result.warnings,
            "audioBase64": None,
        }
    audio = None
    if result.audio is not None and result.sample_rate is not None:
        audio = base64.b64encode(write_wav(result.audio, result.sample_rate)).decode("ascii")
    return {
        "status": result.status,
        "originalText": result.original_text,
        "translatedText": result.translated_text,
        "warnings": result.warnings,
        "audioBase64": audio,
    }


def _pipeline():
    try:
        return build_pipeline()
    except ProviderNotConfigured as exc:
        raise HTTPException(status_code=503, detail={"code": "PROVIDER_NOT_CONFIGURED", "message": str(exc)}) from exc
