class ContextManager:
    """Keeps a short window of completed sentences for providers that can use it.

    The window is bounded so a call does not accumulate a personal transcript.
    """

    def __init__(self, limit: int = 4) -> None:
        self.limit = limit
        self._pairs: list[str] = []

    def recent(self) -> list[str]:
        return list(self._pairs)

    def remember(self, source: str, translated: str) -> None:
        self._pairs.append(f"{source} => {translated}")
        if len(self._pairs) > self.limit:
            self._pairs = self._pairs[-self.limit :]

    def clear(self) -> None:
        self._pairs.clear()
