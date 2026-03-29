CREATE TABLE walkthrough_comments
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_id    UUID         NOT NULL,
    user_id           UUID         NOT NULL,
    content           TEXT         NOT NULL,
    github_comment_id BIGINT,
    sync_status       VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_wc_walkthrough_id ON walkthrough_comments (walkthrough_id);
CREATE INDEX idx_wc_sync_status ON walkthrough_comments (sync_status);
