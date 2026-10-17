-- Players in each game. Runs after 03-create-games.sql (name order).
--
-- The organizer is added as a player when they create the game, so a game's
-- player count ("8/12") is just the number of rows here and is compared with
-- games.max_players. Players are added here when the organizer approves their
-- join request.

CREATE TABLE IF NOT EXISTS game_players (
    game_id   INT UNSIGNED NOT NULL,
    user_id   INT UNSIGNED NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- a user can only be in a game once
    PRIMARY KEY (game_id, user_id),
    -- games a user joined (Home screen)
    KEY idx_game_players_user (user_id),
    CONSTRAINT fk_game_players_game FOREIGN KEY (game_id)
        REFERENCES games (game_id) ON DELETE CASCADE,
    CONSTRAINT fk_game_players_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
