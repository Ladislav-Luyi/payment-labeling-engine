-- Flyway migration V3: Modify aggregates to support label sets
-- Changes aggregates from single label to multiple labels (label sets)

-- First, drop the dependent view
DROP VIEW IF EXISTS v_aggregate_summary;

-- Clear existing aggregate data (since structure is changing)
DELETE FROM aggregates;

-- Drop the old unique constraint
ALTER TABLE aggregates DROP CONSTRAINT IF EXISTS uk_aggregate_unique;

-- Drop the foreign key to label_id
ALTER TABLE aggregates DROP CONSTRAINT IF EXISTS aggregates_label_id_fkey;

-- Drop the old index
DROP INDEX IF EXISTS idx_aggregates_label_id;

-- Drop the label_id column
ALTER TABLE aggregates DROP COLUMN IF EXISTS label_id;

-- Create junction table for aggregate_labels (many-to-many)
CREATE TABLE IF NOT EXISTS aggregate_labels (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL REFERENCES aggregates(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    CONSTRAINT uk_aggregate_label_unique UNIQUE (aggregate_id, label_id)
);

CREATE INDEX idx_aggregate_labels_aggregate_id ON aggregate_labels(aggregate_id);
CREATE INDEX idx_aggregate_labels_label_id ON aggregate_labels(label_id);

-- Create junction table for aggregate_payments (many-to-many)
-- This tracks which payments belong to which aggregates
CREATE TABLE IF NOT EXISTS aggregate_payments (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL REFERENCES aggregates(id) ON DELETE CASCADE,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT uk_aggregate_payment_unique UNIQUE (aggregate_id, payment_id)
);

CREATE INDEX idx_aggregate_payments_aggregate_id ON aggregate_payments(aggregate_id);
CREATE INDEX idx_aggregate_payments_payment_id ON aggregate_payments(payment_id);

-- Recreate the aggregate summary view with new structure
-- Now shows all labels for each aggregate as a comma-separated list
CREATE VIEW v_aggregate_summary AS
SELECT
    a.id,
    COALESCE(
        (SELECT STRING_AGG(l.name, ', ' ORDER BY l.name)
         FROM aggregate_labels al
         JOIN labels l ON al.label_id = l.id
         WHERE al.aggregate_id = a.id
        ),
        'No Labels'
    ) AS label_names,
    a."year",
    a."month",
    a.total_amount,
    a.transaction_count,
    CAST(a."year" AS VARCHAR) || '-' || LPAD(CAST(a."month" AS VARCHAR), 2, '0') AS period
FROM aggregates a
ORDER BY a."year" DESC, a."month" DESC;



