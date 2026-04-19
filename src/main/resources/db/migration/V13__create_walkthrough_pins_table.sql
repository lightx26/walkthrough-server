CREATE TABLE walkthrough_pins (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    walkthrough_id  UUID NOT NULL REFERENCES walkthroughs(id) ON DELETE CASCADE,
    sort_order      INT  NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, walkthrough_id),
    UNIQUE (user_id, sort_order)
);

CREATE INDEX ix_walkthrough_pins_user ON walkthrough_pins(user_id, sort_order);
