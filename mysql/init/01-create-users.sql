-- Runs automatically the first time the mysql-db container starts with an empty
-- data volume (files in /docker-entrypoint-initdb.d run in name order, against
-- MYSQL_DATABASE). It does NOT run again on an existing volume.

CREATE TABLE IF NOT EXISTS users (
    -- unique id for the user inside the app
    user_id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    -- Google's stable account id (the "sub" claim of the id token), used to find the user on sign in
    google_id  VARCHAR(255) NOT NULL,
    email      VARCHAR(320) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    birth_year SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_google_id (google_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
