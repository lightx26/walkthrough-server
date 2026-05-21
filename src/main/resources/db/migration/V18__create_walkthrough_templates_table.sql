CREATE TABLE walkthrough_templates
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    pr_type     VARCHAR(20),
    is_builtin  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_walkthrough_templates_builtin_owner
        CHECK ((is_builtin = TRUE AND user_id IS NULL)
            OR (is_builtin = FALSE AND user_id IS NOT NULL))
);

CREATE INDEX idx_walkthrough_templates_user_id ON walkthrough_templates (user_id);
CREATE INDEX idx_walkthrough_templates_builtin ON walkthrough_templates (is_builtin) WHERE is_builtin = TRUE;
