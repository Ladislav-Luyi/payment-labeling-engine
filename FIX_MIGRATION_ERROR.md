# Fix for Failed V3 Migration

## Problem
The V3 migration failed because it tried to drop `label_id` column, but a view `v_aggregate_summary` depends on it.

## Solution Applied
Updated the V3 migration script to:
1. Drop the dependent view first
2. Drop the label_id column
3. Create new junction tables
4. Recreate the view with new structure

## Steps to Fix

### Option 1: Clean Database Reset (Recommended if no important data)

```bash
# Stop the application if running

# Connect to PostgreSQL and run:
psql -U postgres -d payment_labeling_db

# In psql:
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

# Exit psql
\q

# Now restart the application - all migrations will run fresh
```

### Option 2: Fix the Failed Migration (If you have data to preserve)

```bash
# Connect to PostgreSQL
psql -U postgres -d payment_labeling_db

# Run this SQL:
DELETE FROM flyway_schema_history WHERE version = '3';

# Exit psql
\q

# Now restart the application - V3 will run again with the fixed script
```

### Option 3: Manual SQL Fix (Advanced)

```sql
-- Connect to database
psql -U postgres -d payment_labeling_db

-- Run the complete cleanup:

-- 1. Drop the view
DROP VIEW IF EXISTS v_aggregate_summary;

-- 2. Clear aggregates
DELETE FROM aggregates;

-- 3. Drop constraints
ALTER TABLE aggregates DROP CONSTRAINT IF EXISTS uk_aggregate_unique;
ALTER TABLE aggregates DROP CONSTRAINT IF EXISTS aggregates_label_id_fkey;

-- 4. Drop index
DROP INDEX IF EXISTS idx_aggregates_label_id;

-- 5. Drop column
ALTER TABLE aggregates DROP COLUMN IF EXISTS label_id;

-- 6. Create junction tables
CREATE TABLE IF NOT EXISTS aggregate_labels (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL REFERENCES aggregates(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    CONSTRAINT uk_aggregate_label_unique UNIQUE (aggregate_id, label_id)
);

CREATE INDEX idx_aggregate_labels_aggregate_id ON aggregate_labels(aggregate_id);
CREATE INDEX idx_aggregate_labels_label_id ON aggregate_labels(label_id);

CREATE TABLE IF NOT EXISTS aggregate_payments (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL REFERENCES aggregates(id) ON DELETE CASCADE,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT uk_aggregate_payment_unique UNIQUE (aggregate_id, payment_id)
);

CREATE INDEX idx_aggregate_payments_aggregate_id ON aggregate_payments(aggregate_id);
CREATE INDEX idx_aggregate_payments_payment_id ON aggregate_payments(payment_id);

-- 7. Recreate view
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

-- 8. Mark migration as successful
UPDATE flyway_schema_history 
SET success = true 
WHERE version = '3';

-- Exit
\q
```

## Recommended Approach

**Use Option 2** - it's the cleanest:

```bash
# 1. Connect to database
psql -U postgres -d payment_labeling_db

# 2. Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = '3';

# 3. Exit
\q

# 4. Restart your Spring Boot application
# The fixed V3 migration will now run successfully
```

## After Fix

Once the migration runs successfully:
1. Navigate to http://localhost:8080/aggregates
2. Click "Recalculate Aggregates" button
3. Test the new label-set based aggregation feature

## Files Modified
- `V3__Modify_aggregates_for_label_sets.sql` - Fixed to drop view first

## What Changed in V3 Migration
- Now drops `v_aggregate_summary` view BEFORE dropping `label_id` column
- Recreates the view with new structure that supports multiple labels
- View now shows comma-separated list of all labels for each aggregate

