-- Change walkthrough_file_id FK from ON DELETE CASCADE to ON DELETE SET NULL so that
-- editing a walkthrough (which rebuilds file entities) does not cascade-delete risk zones.
-- WalkthroughServiceImpl.update() re-links risk zones to the new file IDs by filename after rebuild.

ALTER TABLE risk_zones
    ALTER COLUMN walkthrough_file_id DROP NOT NULL;

ALTER TABLE risk_zones
    DROP CONSTRAINT IF EXISTS risk_zones_walkthrough_file_id_fkey;

ALTER TABLE risk_zones
    ADD CONSTRAINT risk_zones_walkthrough_file_id_fkey
        FOREIGN KEY (walkthrough_file_id)
            REFERENCES walkthrough_files (id)
            ON DELETE SET NULL;
