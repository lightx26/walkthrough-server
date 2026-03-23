CREATE TABLE walkthrough_chapters
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_id UUID         NOT NULL,
    title          VARCHAR(500) NOT NULL,
    description    TEXT,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE
);

CREATE INDEX idx_chapters_walkthrough_id ON walkthrough_chapters (walkthrough_id);
