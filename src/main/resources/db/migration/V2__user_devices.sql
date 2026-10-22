CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    push_token VARCHAR(4096) NOT NULL,
    app_version VARCHAR(30),
    device_name VARCHAR(120),
    enabled BOOLEAN NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT user_devices_platform_check CHECK (platform IN ('IOS', 'ANDROID')),
    CONSTRAINT uk_user_devices_user_platform_token UNIQUE (user_account_id, platform, push_token)
);

CREATE INDEX idx_user_devices_user_enabled ON user_devices(user_account_id, enabled);
