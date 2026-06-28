ALTER TABLE walkthrough_review_decisions
    ADD COLUMN github_review_id BIGINT,
    ADD COLUMN sync_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING';
