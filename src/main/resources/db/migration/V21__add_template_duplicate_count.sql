ALTER TABLE walkthrough_templates
    ADD COLUMN duplicate_count BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_walkthrough_templates_duplicate_count
    ON walkthrough_templates (duplicate_count DESC)
    WHERE is_builtin = TRUE;
