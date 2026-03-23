CREATE TABLE walkthroughs
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    owner       VARCHAR(255) NOT NULL,
    repo        VARCHAR(255) NOT NULL,
    pr_number   INTEGER      NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'draft',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_walkthroughs_user_id ON walkthroughs (user_id);
CREATE INDEX idx_walkthroughs_pr ON walkthroughs (owner, repo, pr_number);
