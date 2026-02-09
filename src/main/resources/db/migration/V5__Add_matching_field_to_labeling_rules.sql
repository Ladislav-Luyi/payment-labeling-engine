-- Flyway migration V5: Add matching_field column to labeling_rules
-- Allows rules to specify which payment field to match against

ALTER TABLE labeling_rules ADD COLUMN IF NOT EXISTS matching_field VARCHAR(50) NOT NULL DEFAULT 'counterpartyName';

-- Create index on matching_field for performance
CREATE INDEX IF NOT EXISTS idx_labeling_rules_matching_field ON labeling_rules(matching_field);

