CREATE TABLE languages (
    code VARCHAR(16) PRIMARY KEY,
    english_name VARCHAR(64) NOT NULL,
    native_name VARCHAR(64) NOT NULL,
    translation_code VARCHAR(16) NOT NULL,
    speech_enabled BOOLEAN NOT NULL,
    translation_enabled BOOLEAN NOT NULL,
    tts_enabled BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    phone_number VARCHAR(64) NOT NULL UNIQUE,
    phone_verified BOOLEAN NOT NULL,
    preferred_language VARCHAR(16) NOT NULL REFERENCES languages (code),
    profile_image_url VARCHAR(512),
    translation_consent BOOLEAN NOT NULL,
    terms_accepted_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    preferred_voice VARCHAR(80),
    voice_gender VARCHAR(16) NOT NULL,
    subtitle_enabled BOOLEAN NOT NULL,
    original_audio_enabled BOOLEAN NOT NULL,
    translation_speed DOUBLE PRECISION NOT NULL,
    notifications_enabled BOOLEAN NOT NULL,
    translation_history_enabled BOOLEAN NOT NULL
);

CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id),
    contact_user_id UUID NOT NULL REFERENCES users (id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT contacts_owner_contact_unique UNIQUE (owner_id, contact_user_id)
);

CREATE TABLE call_sessions (
    id UUID PRIMARY KEY,
    caller_id UUID NOT NULL REFERENCES users (id),
    receiver_id UUID NOT NULL REFERENCES users (id),
    caller_language VARCHAR(16) NOT NULL,
    receiver_language VARCHAR(16) NOT NULL,
    room_name VARCHAR(80) NOT NULL UNIQUE,
    dispatch_id VARCHAR(80),
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    duration_seconds INTEGER,
    end_reason VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX call_sessions_caller_idx ON call_sessions (caller_id, created_at DESC);
CREATE INDEX call_sessions_receiver_idx ON call_sessions (receiver_id, created_at DESC);
CREATE INDEX call_sessions_status_idx ON call_sessions (status);

CREATE TABLE call_participants (
    id UUID PRIMARY KEY,
    call_id UUID NOT NULL REFERENCES call_sessions (id),
    user_id UUID NOT NULL REFERENCES users (id),
    participant_role VARCHAR(20) NOT NULL,
    connection_status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ
);

CREATE TABLE translation_events (
    id UUID PRIMARY KEY,
    call_id UUID NOT NULL REFERENCES call_sessions (id),
    speaker_id UUID NOT NULL REFERENCES users (id),
    source_language VARCHAR(16) NOT NULL,
    target_language VARCHAR(16) NOT NULL,
    original_text TEXT NOT NULL,
    translated_text TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX translation_events_call_idx ON translation_events (call_id, created_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    notification_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE consent_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    consent_type VARCHAR(40) NOT NULL,
    granted BOOLEAN NOT NULL,
    policy_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
