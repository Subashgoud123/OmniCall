# Privacy

OmniCall is an interpreter, not a private pipe between two phones.

The translator must receive each speaker's audio to recognize it, translate the sentence, and synthesize the other language. Tell users that before they consent. Do not describe calls as end-to-end encrypted.

What is stored by default:

- account, phone number, language, and call metadata
- consent records
- device push tokens the app registers

What is not stored by default:

- raw call audio
- translation sentences

Translation history is off until a person enables it. Stored sentences are removed on the retention schedule and when that person deletes the account. Deletion asks for the confirmation text `DELETE`, then replaces the name, removes contacts, push tokens, preferences, and that person's translation rows.

Verification codes are hashed in Redis and expire. They are written to the API log only by the development SMS sender. Production must use a real SMS provider.

Cloud credentials, database passwords, LiveKit secrets, and JWT signing keys stay in the environment. They are not compiled into the app.
