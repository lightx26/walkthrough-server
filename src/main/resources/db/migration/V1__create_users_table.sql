CREATE TABLE users
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id           BIGINT       NOT NULL UNIQUE,
    username            VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    email               VARCHAR(255),
    avatar_url          VARCHAR(512),
    github_access_token VARCHAR(512),
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_github_id ON users (github_id);
CREATE INDEX idx_users_username ON users (username);
