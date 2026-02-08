-- Manual cleanup script for failed V3 migration
-- Run this against your PostgreSQL database if the migration failed

-- First, delete the failed migration record from Flyway's history
DELETE FROM flyway_schema_history WHERE version = '3';

-- Now you can restart the application and V3 will run again with the fixed script

