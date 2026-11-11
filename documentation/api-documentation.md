# API

Authenticated requests send `Authorization: Bearer <accessToken>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Start registration and send a code |
| POST | `/api/auth/login` | Send a code when the number is registered |
| POST | `/api/auth/verify-otp` | Exchange the code for tokens |
| POST | `/api/auth/refresh` | Rotate tokens |
| POST | `/api/auth/logout` | Revoke the session |
| GET | `/api/languages` | Language registry |
| GET | `/api/users/me` | Current profile and preferences |
| PUT | `/api/users/preferences` | Language, voice, subtitles, history, speed |
| GET | `/api/users/search?query=` | Find a registered person |
| GET | `/api/users/{userId}` | Public profile |
| POST | `/api/users/me/avatar` | JPEG or PNG profile image |
| DELETE | `/api/users/me` | Delete the account. Body: `{"confirmation":"DELETE"}` |
| GET | `/api/contacts` | Contact list |
| POST | `/api/contacts/invite` | Invite by user id |
| POST | `/api/contacts/{userId}/accept` | Accept an invitation |
| DELETE | `/api/contacts/{userId}` | Remove a contact |
| POST | `/api/calls` | Start a call |
| POST | `/api/calls/{callId}/accept` | Accept |
| POST | `/api/calls/{callId}/reject` | Decline |
| POST | `/api/calls/{callId}/end` | Hang up |
| GET | `/api/calls/history` | Call history |
| GET | `/api/calls/{callId}` | One call |
| GET | `/api/calls/{callId}/token` | Short-lived LiveKit token |
| POST | `/api/devices` | Store an FCM token |
| GET | `/api/notifications` | Stored notifications |
| POST | `/api/translation/text` | Authenticated text test, proxied to the AI service |
| POST | `/api/translation/speech` | Authenticated WAV test, proxied to the AI service |
| GET | `/ws?token=` | Call signaling |

`POST /api/internal/calls/{callId}/translations` is for the worker. It requires `X-OmniCall-Internal-Token`.

Errors use `{ "code", "message", "timestamp" }`.

A call stays `RINGING` until it is accepted, declined, cancelled, or missed after the ring timeout. `ACTIVE` is the only state that issues a LiveKit token.

The speech and text routes are for measuring a provider. They are not on the live call path.
