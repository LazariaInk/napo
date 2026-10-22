ALTER TABLE user_devices
    ADD COLUMN installation_id VARCHAR(120);

ALTER TABLE user_devices
    ADD CONSTRAINT uk_user_devices_user_platform_installation
    UNIQUE (user_account_id, platform, installation_id);

CREATE INDEX idx_user_devices_installation_id
    ON user_devices(user_account_id, platform, installation_id);
