CREATE TABLE chapter_read_marks
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    walkthrough_id  UUID        NOT NULL,
    chapter_id      UUID        NOT NULL,
    marked_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES walkthrough_chapters (id) ON DELETE CASCADE,

    UNIQUE (user_id, chapter_id)
);

CREATE INDEX idx_crm_user_id ON chapter_read_marks (user_id);
CREATE INDEX idx_crm_walkthrough_id ON chapter_read_marks (walkthrough_id);
CREATE INDEX idx_crm_chapter_id ON chapter_read_marks (chapter_id);

-- Backfill marks from prior marked_as_read view events
INSERT INTO chapter_read_marks (user_id, walkthrough_id, chapter_id, marked_at)
SELECT cve.user_id,
       c.walkthrough_id,
       cve.chapter_id,
       MIN(cve.viewed_at) AS marked_at
FROM chapter_view_events cve
JOIN walkthrough_chapters c ON c.id = cve.chapter_id
WHERE cve.marked_as_read = TRUE
GROUP BY cve.user_id, c.walkthrough_id, cve.chapter_id
ON CONFLICT (user_id, chapter_id) DO NOTHING;

-- Mark state no longer lives on view events
ALTER TABLE chapter_view_events DROP COLUMN marked_as_read;

-- Recompute read_progress.read_chapters from the new source of truth
UPDATE read_progress rp
SET read_chapters = COALESCE(sub.cnt, 0)
FROM (
    SELECT m.user_id, m.walkthrough_id, COUNT(*)::int AS cnt
    FROM chapter_read_marks m
    GROUP BY m.user_id, m.walkthrough_id
) sub
WHERE sub.user_id = rp.user_id
  AND sub.walkthrough_id = rp.walkthrough_id;

UPDATE read_progress
SET read_chapters = 0
WHERE NOT EXISTS (
    SELECT 1 FROM chapter_read_marks m
    WHERE m.user_id = read_progress.user_id
      AND m.walkthrough_id = read_progress.walkthrough_id
);
