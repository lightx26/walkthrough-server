ALTER TABLE walkthrough_files
    ADD COLUMN raw_patch TEXT;

ALTER TABLE walkthroughs
    ADD COLUMN commit_sha VARCHAR(255);
