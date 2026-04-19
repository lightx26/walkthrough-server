CREATE TABLE activity_entries (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type        VARCHAR(48) NOT NULL,
    occurred_at       TIMESTAMPTZ NOT NULL,
    walkthrough_id    UUID,
    chapter_id        UUID,
    comment_id        UUID,
    parent_comment_id UUID,
    target_user_id    UUID,
    metadata          JSONB NOT NULL DEFAULT '{}',
    visibility        VARCHAR(8) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_activity_user_time ON activity_entries (user_id, occurred_at DESC);
CREATE INDEX ix_activity_user_type_time ON activity_entries (user_id, event_type, occurred_at DESC);
