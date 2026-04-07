ALTER TABLE walkthrough_comments
    ADD COLUMN chapter_id          UUID,
    ADD COLUMN walkthrough_file_id UUID,
    ADD COLUMN diff_position       INTEGER,
    ADD COLUMN parent_id           UUID;

ALTER TABLE walkthrough_comments
    ADD CONSTRAINT fk_wc_chapter FOREIGN KEY (chapter_id) REFERENCES walkthrough_chapters (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_wc_file FOREIGN KEY (walkthrough_file_id) REFERENCES walkthrough_files (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_wc_parent FOREIGN KEY (parent_id) REFERENCES walkthrough_comments (id) ON DELETE CASCADE;

CREATE INDEX idx_wc_chapter_id ON walkthrough_comments (chapter_id);
CREATE INDEX idx_wc_file_id ON walkthrough_comments (walkthrough_file_id);
CREATE INDEX idx_wc_parent_id ON walkthrough_comments (parent_id);
