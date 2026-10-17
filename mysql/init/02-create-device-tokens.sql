-- Firebase Cloud Messaging tokens, one row per app install. Used to send push
-- notifications to every device a user is signed into. Runs after
-- 01-create-users.sql (name order) since it references users.

CREATE TABLE IF NOT EXISTS device_tokens (
    -- FCM registration token, identifies the app on one device
    token      VARCHAR(512) CHARACTER SET ascii NOT NULL,
    -- user currently signed in on that device, a token moves to whoever registers it last
    user_id    INT UNSIGNED NOT NULL,
    -- refreshed every time the app registers the token, used to clean up stale devices
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (token),
    KEY idx_device_tokens_user_id (user_id),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
