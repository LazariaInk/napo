ALTER TABLE user_accounts
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_user_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN,
    ADD COLUMN IF NOT EXISTS plan VARCHAR(30);

UPDATE user_accounts SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
UPDATE user_accounts SET plan = 'FREE' WHERE plan IS NULL;

ALTER TABLE user_accounts DROP CONSTRAINT IF EXISTS user_accounts_auth_provider_check;
ALTER TABLE user_accounts
    ADD CONSTRAINT user_accounts_auth_provider_check CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));

ALTER TABLE user_accounts DROP CONSTRAINT IF EXISTS user_accounts_plan_check;
ALTER TABLE user_accounts
    ADD CONSTRAINT user_accounts_plan_check CHECK (plan IN ('FREE', 'PLUS', 'BUSINESS'));

ALTER TABLE reminders DROP CONSTRAINT IF EXISTS reminders_preferred_channel_check;
ALTER TABLE reminders
    ADD CONSTRAINT reminders_preferred_channel_check CHECK (preferred_channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG'));

ALTER TABLE reminder_notification_attempts DROP CONSTRAINT IF EXISTS reminder_notification_attempts_channel_check;
ALTER TABLE reminder_notification_attempts
    ADD CONSTRAINT reminder_notification_attempts_channel_check CHECK (channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG'));

ALTER TABLE reminder_notification_attempts DROP CONSTRAINT IF EXISTS reminder_notification_attempts_status_check;
ALTER TABLE reminder_notification_attempts
    ADD CONSTRAINT reminder_notification_attempts_status_check CHECK (status IN ('SUCCESS', 'SKIPPED', 'FAILED'));

CREATE TABLE IF NOT EXISTS chat_usage_daily (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id),
    usage_date DATE NOT NULL,
    message_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_chat_usage_daily_user_date UNIQUE (user_account_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_chat_usage_daily_user_date ON chat_usage_daily(user_account_id, usage_date);