CREATE TABLE walkthrough_files
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id  UUID          NOT NULL,
    filename    VARCHAR(1000) NOT NULL,
    file_sha    VARCHAR(255)  NOT NULL,
    file_status VARCHAR(50)   NOT NULL,
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (chapter_id) REFERENCES walkthrough_chapters (id) ON DELETE CASCADE
);

CREATE INDEX idx_wt_files_chapter_id ON walkthrough_files (chapter_id);
