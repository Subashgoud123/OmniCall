import io
import wave


def read_wav(data: bytes) -> tuple[bytes, int]:
    with wave.open(io.BytesIO(data), "rb") as wav:
        if wav.getsampwidth() != 2 or wav.getnchannels() != 1:
            raise ValueError("Speech must be mono 16-bit PCM WAV")
        return wav.readframes(wav.getnframes()), wav.getframerate()


def write_wav(pcm: bytes, sample_rate: int) -> bytes:
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(pcm)
    return buffer.getvalue()


def strip_wav_header(data: bytes) -> tuple[bytes, int]:
    if data[:4] == b"RIFF":
        return read_wav(data)
    return data, 24000
