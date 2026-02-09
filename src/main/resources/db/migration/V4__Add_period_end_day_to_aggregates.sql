-- Flyway migration V4: add period end day for custom aggregation windows

ALTER TABLE aggregates
    ADD COLUMN IF NOT EXISTS period_end_day INTEGER;

CREATE INDEX IF NOT EXISTS idx_aggregates_period_end_day
    ON aggregates(period_end_day);

