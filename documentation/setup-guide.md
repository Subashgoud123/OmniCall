# Setup

## 1. API build

The API targets Java 21.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
cd backend\spring-boot-api
mvn test
```

## 2. Data stores and LiveKit

From the repository root:

```powershell
docker compose up postgres redis livekit
```

The local LiveKit key in `infrastructure/livekit/livekit.yaml` is `devkey` / `secret`. It matches `.env.example` and is only for this machine. Replace both before any shared deployment.

Copy `.env.example` to `.env` when you want the API container to use the same values.

## 3. API process

With Postgres on `localhost:5432` and Redis on `localhost:6379`:

```powershell
cd backend\spring-boot-api
mvn spring-boot:run
```

The dev profile logs SMS codes. That sender refuses to start when the `prod` profile is active. For a real SMS provider set `SMS_PROVIDER=twilio` and the Twilio account values.

Open `http://localhost:8080/swagger-ui.html` for the API.

## 4. Translator

```powershell
cd ai-service\python
py -3.13 -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
.\.venv\Scripts\python -m pytest
.\.venv\Scripts\uvicorn app.main:app --port 8090
```

A second process joins LiveKit rooms:

```powershell
$env:LIVEKIT_URL = "ws://localhost:7880"
$env:LIVEKIT_API_KEY = "devkey"
$env:LIVEKIT_API_SECRET = "secret"
$env:GOOGLE_CLOUD_PROJECT = "your-project"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\service-account.json"
.\.venv\Scripts\python -m app.worker dev
```

Without the Google variables, `/ready` reports degraded and the worker will not invent audio. With them, `POST /api/translation/speech` accepts a mono 16-bit WAV and returns translated speech. Live calls do not use that endpoint. They use the worker.

Google Speech-to-Text, Translation, and Text-to-Speech are the first providers. Hindi and Telugu voices are selected in `app/config/languages.py`.

## 5. Flutter

```powershell
$env:Path = "$env:USERPROFILE\sdk\flutter\bin;" + $env:Path
cd mobile\omnicall_flutter
flutter pub get
flutter test
flutter run --dart-define=API_BASE=http://10.0.2.2:8080 --dart-define=WS_BASE=ws://10.0.2.2:8080/ws
```

`10.0.2.2` is the Android emulator's address for the host. A physical phone needs this computer's LAN address, and the API must be reachable on that interface.

The Android project is created with `flutter create` if `android/` is not present yet:

```powershell
flutter create --org com.omnicall --project-name omnicall_flutter --platforms android,ios .
```

Then set `minSdk` to 24 and add the call foreground service described in the deployment guide.

## Checks that do not need cloud credentials

- `mvn test` for token signing, phone numbers, and language direction
- `pytest` for sentence protection, voice activity, routing, duplicate suppression, and a failed translation
- `flutter test` for duration formatting and the welcome screen
