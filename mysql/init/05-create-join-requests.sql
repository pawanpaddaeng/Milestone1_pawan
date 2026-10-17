-- Requests to join a game, with the answers from the join request popup.
-- Runs after 03-create-games.sql (name order).
--
-- A user has at most one request per game. Asking again (e.g. after cancelling
-- or being declined) updates that row back to pending instead of adding a new one.
-- Approving a request also adds the user to game_players.

CREATE TABLE IF NOT EXISTS join_requests (
    request_id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    game_id        INT UNSIGNED NOT NULL,
    -- user asking to join
    user_id        INT UNSIGNED NOT NULL,

    -- answers to the popup's questions, shown to the organizer
    skill_level    ENUM('beginner', 'intermediate', 'advanced', 'competitive') NOT NULL,
    play_frequency ENUM('rarely', 'monthly', 'weekly', 'several_per_week') NULL,
    message        VARCHAR(200) NULL,

    -- pending until the organizer responds; cancelled when the requester withdraws it
    status         ENUM('pending', 'approved', 'declined', 'cancelled') NOT NULL DEFAULT 'pending',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- when the organizer approved or declined, null while pending
    responded_at   TIMESTAMP NULL,

    PRIMARY KEY (request_id),
    UNIQUE KEY uq_join_requests_game_user (game_id, user_id),
    -- organizer's pending requests for a game and the badge count
    KEY idx_join_requests_game_status (game_id, status, created_at),
    -- a user's own requests (e.g. "Request pending" on Discover)
    KEY idx_join_requests_user (user_id, status),
    CONSTRAINT fk_join_requests_game FOREIGN KEY (game_id)
        REFERENCES games (game_id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
