CREATE TABLE read_progress
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    walkthrough_id  UUID        NOT NULL,
    last_chapter_id UUID,
    read_chapters   INTEGER     NOT NULL DEFAULT 0,
    total_chapters  INTEGER     NOT NULL DEFAULT 0,
    time_spent_sec  INTEGER     NOT NULL DEFAULT 0,
    read_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE,
    FOREIGN KEY (last_chapter_id) REFERENCES walkthrough_chapters (id) ON DELETE SET NULL,

    UNIQUE (user_id, walkthrough_id)
);

CREATE INDEX idx_rp_user_id ON read_progress (user_id);
CREATE INDEX idx_rp_walkthrough_id ON read_progress (walkthrough_id);
