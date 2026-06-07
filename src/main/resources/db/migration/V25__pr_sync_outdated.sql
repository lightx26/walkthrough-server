-- PR Sync & OUTDATED Status: replace snapshot/versioning with on-demand file consistency check

-- 1. Add reason column for OUTDATED walkthroughs
ALTER TABLE walkthroughs
    ADD COLUMN outdated_reason TEXT;

-- 2. Drop snapshot infrastructure (replaced by on-demand check)
DROP TABLE IF EXISTS walkthrough_snapshots;

-- 3. Drop version tracking (no longer used)
ALTER TABLE walkthroughs
    DROP COLUMN IF EXISTS version;

-- 4. Drop annotation-level staleness (only meaningful in the old diff view)
ALTER TABLE annotations
    DROP COLUMN IF EXISTS status;
