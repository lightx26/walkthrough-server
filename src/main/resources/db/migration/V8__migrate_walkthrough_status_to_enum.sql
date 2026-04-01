-- Migrate walkthrough status column from VARCHAR to a PostgreSQL enum type

-- 1. Create the enum type
CREATE TYPE walkthrough_status AS ENUM ('DRAFT', 'PUBLISHED');

-- 2. Normalise any existing values to uppercase (handles legacy lowercase 'draft'/'published')
UPDATE walkthroughs
SET status = UPPER(status)
WHERE status != UPPER(status);

-- 3. Drop the existing default so the USING cast is not blocked
ALTER TABLE walkthroughs
    ALTER COLUMN status DROP DEFAULT;

-- 4. Convert the column to the new enum type using the updated values
ALTER TABLE walkthroughs
    ALTER COLUMN status TYPE walkthrough_status
        USING status::walkthrough_status;

-- 5. Restore the default in terms of the new enum type
ALTER TABLE walkthroughs
    ALTER COLUMN status SET DEFAULT 'DRAFT'::walkthrough_status;
