-- Phase 5: Walkthrough Versioning & Diff View

-- 1. Add version column to walkthroughs
ALTER TABLE walkthroughs
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- 2. Add annotation status (ACTIVE / OUTDATED)
ALTER TABLE annotations
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 3. Create walkthrough_snapshots table to capture published structure
CREATE TABLE walkthrough_snapshots
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_id        UUID         NOT NULL,
    version               INTEGER      NOT NULL,
    commit_sha            VARCHAR(255),
    walkthrough_content   JSONB        NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE,
    UNIQUE (walkthrough_id, version)
);

CREATE INDEX idx_walkthrough_snapshots_walkthrough_id ON walkthrough_snapshots (walkthrough_id);
