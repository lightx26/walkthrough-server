CREATE TABLE annotations
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_file_id UUID        NOT NULL,
    start_line          INTEGER     NOT NULL,
    end_line            INTEGER     NOT NULL,
    line_side           VARCHAR(10) NOT NULL DEFAULT 'new',
    content             TEXT        NOT NULL,
    sort_order          INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (walkthrough_file_id) REFERENCES walkthrough_files (id) ON DELETE CASCADE
);

CREATE INDEX idx_annotations_file_id ON annotations (walkthrough_file_id);
