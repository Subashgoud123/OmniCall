import numpy as np


class VoiceActivityDetector:
    def __init__(self, threshold: float = 450.0, start_frames: int = 3, end_frames: int = 20) -> None:
        self.threshold = threshold
        self.start_frames = start_frames
        self.end_frames = end_frames

    def is_speech(self, frame: bytes) -> bool:
        if not frame:
            return False
        samples = np.frombuffer(frame, dtype=np.int16)
        if samples.size == 0:
            return False
        rms = float(np.sqrt(np.mean(samples.astype(np.float32) ** 2)))
        return rms >= self.threshold
