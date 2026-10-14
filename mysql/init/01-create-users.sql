-- Runs automatically the first time the mysql-db container starts with an empty
-- data volume (files in /docker-entrypoint-initdb.d run in name order, against
-- MYSQL_DATABASE). It does NOT run again on an existing volume.

CREATE TABLE IF NOT EXISTS users (
    user_id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
