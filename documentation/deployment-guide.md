# Deployment

## Containers

`docker compose up --build` starts PostgreSQL, Redis, LiveKit, the API, the AI HTTP service, and the translator worker.

Mount a real Google credential file into the AI containers and set `GOOGLE_APPLICATION_CREDENTIALS` to that path. Do not bake the file into the image.

Set these before any shared environment:

- `JWT_SECRET` to a random string of at least 32 bytes
- `INTERNAL_API_TOKEN`
- `LIVEKIT_API_KEY` and `LIVEKIT_API_SECRET`, and the same pair in the LiveKit config
- `SMS_PROVIDER=twilio` with Twilio credentials
- `SPRING_PROFILES_ACTIVE=prod`

The production profile refuses the log SMS sender, the sample JWT secret, the sample internal token, and the local LiveKit secret.

## LiveKit

Publish TCP 7880 and 7881, and a UDP port range. Set `use_external_ip: true` and the public address when the server is not on the same machine as the phones. The sample range `50000-50100` is for local development. Widen it for concurrent calls.

The API host must be the HTTP LiveKit URL (`http://livekit:7880` inside Compose). Phones receive the WebSocket URL from `LIVEKIT_URL`.

## Android background calls

After `flutter create`, set `minSdk` 24 and add a microphone foreground service:

- permissions: `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `MODIFY_AUDIO_SETTINGS`, `BLUETOOTH_CONNECT`, `INTERNET`
- service `com.omnicall.omnicall_flutter.CallForegroundService` with `foregroundServiceType="microphone"`
- a method channel `com.omnicall/call_service` with `start`, `stop`, and `setSpeaker`

`start` creates the ongoing notification, starts the foreground service, sets `MODE_IN_COMMUNICATION`, and requests audio focus. `setSpeaker` toggles the speakerphone. Bluetooth headsets follow that communication mode. `stop` abandons focus and stops the service.

Cleartext HTTP is acceptable only in debug. Release builds should use HTTPS and `wss`.

Firebase Cloud Messaging needs `google-services.json` and `FIREBASE_CREDENTIALS_PATH` plus `FIREBASE_PROJECT_ID` on the API. Without them, incoming calls still arrive over the WebSocket while the app is open. Push is not simulated.

## Cost

Each translated utterance spends one recognition, one translation, and one synthesis. The API rejects a new call after the configured daily seconds (`DAILY_CALL_SECONDS`, default 3600 per user). Utterances are capped at 15 seconds and provider calls time out. Measure the actual provider invoice for a Hindi-to-Telugu minute and a Telugu-to-Hindi minute before raising the limit.

## Health

- API: `GET /actuator/health`
- Translator: `GET /health` and `GET /ready`
