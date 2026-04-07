CREATE TABLE chapter_view_events
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id          UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    time_spent_sec      INTEGER     NOT NULL DEFAULT 0,
    scrolled_to_bottom  BOOLEAN     NOT NULL DEFAULT FALSE,
    viewed_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (chapter_id) REFERENCES walkthrough_chapters (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_cve_chapter_id ON chapter_view_events (chapter_id);
CREATE INDEX idx_cve_user_id ON chapter_view_events (user_id);
