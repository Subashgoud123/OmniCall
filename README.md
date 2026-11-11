# OmniCall

Speak your language. Understand everyone.

OmniCall is an Android-first voice calling app for a live conversation between two people who speak different languages. Each person talks normally. A server-side translator recognizes the sentence, translates it, and speaks it in the other person's language.

Hindi (`hi-IN`) and Telugu (`te-IN`) are the enabled languages. More Indian languages are registered and switched off until they are turned on in the language table.

## What a call does

1. Both people sign in with a phone-number code.
2. One person starts a call. The API creates a LiveKit room and dispatches the translator.
3. Each phone publishes its microphone and subscribes to the translated voice for its own language.
4. Original audio stays off unless that person turns it on.
5. Subtitles show the recognized sentence and the translation. A failed translation is shown as unavailable. It is never presented as a successful translation.

Call audio is processed by the translator. These calls are not end-to-end encrypted. Raw audio is not stored.

## Repository

```text
mobile/omnicall_flutter     Flutter app
backend/spring-boot-api     Java 21 / Spring Boot 3 API
ai-service/python           FastAPI health API and LiveKit translator worker
infrastructure/livekit      Local LiveKit config
documentation               Architecture, API, setup, privacy, and tests
```

## Local requirements

- JDK 21 for the API. This machine also has JDK 26; Spring Boot 3.5 does not support 26, so the API build uses JDK 21.
- Maven
- Python 3.12 or 3.13
- Flutter stable on `PATH`
- Docker, for PostgreSQL, Redis, and LiveKit

Google Cloud credentials are required before a real Hindi or Telugu voice can be produced. The app does not invent a translation when those credentials are missing.

## Start here

See [documentation/setup-guide.md](documentation/setup-guide.md).
