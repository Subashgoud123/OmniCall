import re

NUMBER_PATTERN = re.compile(
    r"(?<!\w)(?:\+\d{1,3}(?:[\s-]\d{2,})+|\d{1,4}:\d{2}|\d{1,4}(?:[./-]\d{1,4})+)(?!\w)|(?<!\w)\d+(?!\w)"
)
TOKEN = re.compile(r"__OC(\d+)__")


def protect(text: str, terms: list[str]) -> tuple[str, list[str]]:
    """Replace numbers and known names so the translator cannot rewrite them."""
    replacements: list[str] = []

    def take(value: str) -> str:
        replacements.append(value)
        return f"__OC{len(replacements) - 1}__"

    protected = NUMBER_PATTERN.sub(lambda match: take(match.group(0)), text)
    for term in sorted({term.strip() for term in terms if term and term.strip()}, key=len, reverse=True):
        if term in protected:
            protected = protected.replace(term, take(term))
    return protected, replacements


def restore(text: str, replacements: list[str]) -> tuple[str, list[str]]:
    missing: list[str] = []
    restored = text
    for index, value in enumerate(replacements):
        token = f"__OC{index}__"
        if token not in restored:
            missing.append(value)
        else:
            restored = restored.replace(token, value)
    return restored, missing
