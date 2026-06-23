CREATE TABLE walkthrough_review_decisions
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_id UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    decision       VARCHAR(20) NOT NULL,
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_review_decision_user UNIQUE (walkthrough_id, user_id),
    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE
);

CREATE INDEX idx_review_decisions_walkthrough_id ON walkthrough_review_decisions (walkthrough_id);
