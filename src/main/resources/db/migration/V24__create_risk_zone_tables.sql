CREATE TABLE risk_scans
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walkthrough_id UUID        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider       VARCHAR(50),
    model          VARCHAR(100),
    total_files    INTEGER     NOT NULL DEFAULT 0,
    analyzed_files INTEGER     NOT NULL DEFAULT 0,
    file_progress  JSONB,
    critical_count INTEGER     NOT NULL DEFAULT 0,
    high_count     INTEGER     NOT NULL DEFAULT 0,
    medium_count   INTEGER     NOT NULL DEFAULT 0,
    low_count      INTEGER     NOT NULL DEFAULT 0,
    error_message  TEXT,
    triggered_by   UUID        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (walkthrough_id) REFERENCES walkthroughs (id) ON DELETE CASCADE
);

CREATE INDEX idx_risk_scans_walkthrough_id ON risk_scans (walkthrough_id);

CREATE TABLE risk_zones
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_scan_id        UUID         NOT NULL,
    walkthrough_file_id UUID         NOT NULL,
    risk_level          VARCHAR(10)  NOT NULL,
    category            VARCHAR(50)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    suggestion          TEXT,
    start_position      INTEGER,
    end_position        INTEGER,
    line_side           VARCHAR(10),
    review_status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (risk_scan_id)        REFERENCES risk_scans (id)         ON DELETE CASCADE,
    FOREIGN KEY (walkthrough_file_id) REFERENCES walkthrough_files (id)  ON DELETE CASCADE
);

CREATE INDEX idx_risk_zones_scan_id ON risk_zones (risk_scan_id);
CREATE INDEX idx_risk_zones_file_id ON risk_zones (walkthrough_file_id);
