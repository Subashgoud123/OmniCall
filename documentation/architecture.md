# Architecture

OmniCall splits business state from media.

```text
Flutter app
  REST + WebSocket  ->  Spring Boot API  ->  PostgreSQL
                    ->  Redis (codes, sessions, usage)
                    ->  LiveKit room token
LiveKit room
  microphone tracks ->  Python translator worker
  translated tracks ->  the other phone
  subtitle data     ->  both phones
  optional history  ->  API, only with consent
```

## Languages

The enabled set lives in `languages` and is seeded by Flyway. Hindi and Telugu start enabled. English, Tamil, Kannada, Malayalam, Marathi, Bengali, Gujarati, and Punjabi are present and disabled. The call direction is "speaker language to the other participant's language". Nothing in the call flow special-cases Hindi or Telugu beyond the registry and the voice map.

## Audio routing

Each person publishes one microphone track. The worker publishes one translated track per speaker, named `tx:<listener language>`.

- A Hindi speaker publishes speech that becomes `tx:te-IN`.
- A Telugu speaker publishes speech that becomes `tx:hi-IN`.
- The worker asks LiveKit to subscribe the listener to the translated track and to keep the speaker off that track.
- Phones also refuse the other subscription locally. Original audio is subscribed only when the listener has turned that preference on.

The worker waits for a voice-activity boundary before translating. It does not translate each audio packet. A newer utterance cancels an older synthesis so an interruption does not play a stale sentence.

Numbers and the participants' display names are held out of the translator with placeholders and put back afterward. If a placeholder does not survive, the subtitle carries a warning.

## Trust boundary

LiveKit access tokens are minted by the API. The phone never sees the LiveKit secret. Google credentials stay on the AI service. The worker reports subtitles to the API with an internal token.

Translation text is stored only when the speaker has enabled history. A daily job deletes events older than the retention window. Account deletion removes that speaker's events and anonymizes the user row.
