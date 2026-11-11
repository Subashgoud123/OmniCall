# Testing

## Automated

| Check | Command | Covers |
|---|---|---|
| API | `mvn test` in `backend/spring-boot-api` | JWT round trip, phone format, Hindi/Telugu direction |
| Translator | `pytest` in `ai-service/python` | number protection, context limit, failed translation, voice activity, track routing, duplicates, interruption |
| App | `flutter test` in `mobile/omnicall_flutter` | duration format, language direction, welcome screen |

A failed provider test expects `translatedText` to stay empty. The original sentence is kept so the UI can show it and say that translated audio is unavailable.

## Provider trial

With Google credentials set, record a mono 16 kHz 16-bit WAV and post it to the AI service:

```powershell
curl.exe -F "sourceLanguage=hi-IN" -F "targetLanguage=te-IN" -F "file=@hindi.wav" http://localhost:8090/api/translation/speech
```

Repeat with the languages swapped. This exercises recognition, translation, and synthesis. It does not exercise the call.

## Two phones

1. Register one account in Hindi and one in Telugu. Read the development SMS code from the API log, or use Twilio.
2. Confirm both accounts accepted translation consent.
3. Start LiveKit, the API, and `python -m app.worker dev`.
4. From the Hindi phone, search the Telugu number and call.
5. Accept on the Telugu phone.
6. Say a short sentence. The other phone should hear one translated voice, not the original and the translation together.
7. Mute, then unmute.
8. Speak over the translation and confirm the older sentence stops.
9. Turn the screen off and confirm the ongoing-call notification is present.
10. Hang up and confirm the duration is in history.

## Sentences for native listeners

Use these as a listening script, not as golden strings baked into a unit test:

- Greeting and a short reply.
- "आपका घर कहाँ है? मुझे लोकेशन बताइए।"
- "మా ఇల్లు మెయిన్ రోడ్ పక్కన ఉంది."
- An address with a building number.
- A person's name.
- A time, a date, and a phone number.
- A food-delivery instruction with a quantity.
- A direction: left, right, next to, opposite.
- A question and a one-word answer.
- The same lines with a fan or street noise behind the speaker.

Ask a Hindi speaker and a Telugu speaker whether the meaning, the number, and the name survived. Record the provider latency separately for each direction. The engineering target is about one to two seconds in good network conditions. That is a target, not a guarantee of the speech provider.

## Not covered until devices and credentials are available

- A real Google round trip
- Firebase delivery while the app is killed
- Bluetooth route changes
- Lock-screen audio on a specific Android version
