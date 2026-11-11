from app.speech.vad import VoiceActivityDetector


class AudioBuffer:
    def __init__(self, max_bytes: int) -> None:
        self.max_bytes = max_bytes
        self._data = bytearray()

    def append(self, chunk: bytes) -> None:
        self._data.extend(chunk)
        if len(self._data) > self.max_bytes:
            overflow = len(self._data) - self.max_bytes
            del self._data[:overflow]

    def snapshot(self) -> bytes:
        return bytes(self._data)

    def clear(self) -> None:
        self._data.clear()

    def __len__(self) -> int:
        return len(self._data)


class UtteranceSegmenter:
    """Groups microphone frames into sentences instead of translating every packet."""

    def __init__(self, sample_rate: int = 16000, max_seconds: float = 15.0) -> None:
        self.sample_rate = sample_rate
        self.frame_bytes = int(sample_rate * 0.02) * 2
        self.max_bytes = int(sample_rate * max_seconds) * 2
        self.vad = VoiceActivityDetector()
        self._pending = bytearray()
        self._speech = bytearray()
        self._voiced = 0
        self._silence = 0
        self._speaking = False

    def feed(self, pcm: bytes) -> list[bytes]:
        self._pending.extend(pcm)
        completed: list[bytes] = []
        while len(self._pending) >= self.frame_bytes:
            frame = bytes(self._pending[: self.frame_bytes])
            del self._pending[: self.frame_bytes]
            completed.extend(self._consume(frame))
        return completed

    def flush(self) -> list[bytes]:
        if self._speaking and len(self._speech) >= self.frame_bytes * 10:
            utterance = bytes(self._speech)
            self._reset()
            return [utterance]
        self._reset()
        return []

    def _consume(self, frame: bytes) -> list[bytes]:
        speech = self.vad.is_speech(frame)
        if not self._speaking:
            if speech:
                self._voiced += 1
                self._speech.extend(frame)
                if self._voiced >= self.vad.start_frames:
                    self._speaking = True
            else:
                self._voiced = 0
                self._speech.clear()
            return []

        self._speech.extend(frame)
        if speech:
            self._silence = 0
        else:
            self._silence += 1
        if len(self._speech) >= self.max_bytes or self._silence >= self.vad.end_frames:
            utterance = bytes(self._speech)
            self._reset()
            if len(utterance) >= self.frame_bytes * 10:
                return [utterance]
        return []

    def _reset(self) -> None:
        self._speech.clear()
        self._voiced = 0
        self._silence = 0
        self._speaking = False
