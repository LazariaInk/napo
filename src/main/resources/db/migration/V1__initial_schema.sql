CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    auth_provider VARCHAR(30),
    provider_user_id VARCHAR(120),
    email_verified BOOLEAN,
    role VARCHAR(30) NOT NULL,
    plan VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT user_accounts_auth_provider_check CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT user_accounts_role_check CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT user_accounts_plan_check CHECK (plan IN ('FREE', 'PLUS', 'BUSINESS'))
);

CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT REFERENCES user_accounts(id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    remind_at TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recurrence_type VARCHAR(30) NOT NULL,
    recurrence_rule VARCHAR(500),
    preferred_channel VARCHAR(30) NOT NULL,
    message_to_send VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT reminders_status_check CHECK (status IN ('PENDING', 'TRIGGERED', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT reminders_recurrence_type_check CHECK (recurrence_type IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT reminders_preferred_channel_check CHECK (preferred_channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG'))
);

CREATE TABLE reminder_notification_attempts (
    id BIGSERIAL PRIMARY KEY,
    reminder_id BIGINT NOT NULL REFERENCES reminders(id),
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    provider_message_id VARCHAR(200),
    error_message VARCHAR(1000),
    CONSTRAINT reminder_notification_attempts_channel_check CHECK (channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG')),
    CONSTRAINT reminder_notification_attempts_status_check CHECK (status IN ('SUCCESS', 'SKIPPED', 'FAILED'))
);

CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_conversations_status_check CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES chat_conversations(id),
    sender VARCHAR(30) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_messages_sender_check CHECK (sender IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE TABLE chat_usage_daily (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id),
    usage_date DATE NOT NULL,
    message_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_chat_usage_daily_user_date UNIQUE (user_account_id, usage_date)
);

CREATE INDEX idx_reminders_status_remind_at ON reminders(status, remind_at);
CREATE INDEX idx_reminders_user_account_id ON reminders(user_account_id);
CREATE INDEX idx_notification_attempts_reminder_id ON reminder_notification_attempts(reminder_id);
CREATE INDEX idx_chat_conversations_user_account_id ON chat_conversations(user_account_id);
CREATE INDEX idx_chat_messages_conversation_id_created_at ON chat_messages(conversation_id, created_at);
CREATE INDEX idx_chat_usage_daily_user_date ON chat_usage_daily(user_account_id, usage_date);