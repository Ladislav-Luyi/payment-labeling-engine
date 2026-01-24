-- Flyway migration V1: Initial schema for Payment Labeling Engine
-- Creates all necessary tables and indexes for the application

-- Create LABELS table
CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_labels_name ON labels(name);

-- Create LABELING_RULES table
CREATE TABLE labeling_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    regex_pattern TEXT NOT NULL,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_labeling_rules_label_id ON labeling_rules(label_id);
CREATE INDEX idx_labeling_rules_is_active ON labeling_rules(is_active);

-- Create PAYMENTS table with unique constraint for duplicate detection
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_date DATE NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(255),
    transaction_type VARCHAR(100),
    account_number VARCHAR(50),
    counterparty_account VARCHAR(50),
    counterparty_bank VARCHAR(100),
    counterparty_name VARCHAR(255),
    receiver_info TEXT,
    additional_info TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_unique UNIQUE (payment_date, amount, reference, account_number)
);

CREATE INDEX idx_payments_payment_date ON payments(payment_date);
CREATE INDEX idx_payments_amount ON payments(amount);
CREATE INDEX idx_payments_account_number ON payments(account_number);
CREATE INDEX idx_payments_counterparty_name ON payments(counterparty_name);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Create PAYMENT_LABELS junction table (many-to-many relationship)
CREATE TABLE payment_labels (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_label_unique UNIQUE (payment_id, label_id)
);

CREATE INDEX idx_payment_labels_payment_id ON payment_labels(payment_id);
CREATE INDEX idx_payment_labels_label_id ON payment_labels(label_id);

-- Create AGGREGATES table for monthly/yearly aggregation
CREATE TABLE aggregates (
    id BIGSERIAL PRIMARY KEY,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    transaction_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_aggregate_unique UNIQUE (label_id, year, month),
    CONSTRAINT chk_month_range CHECK (month >= 1 AND month <= 12),
    CONSTRAINT chk_year_positive CHECK (year > 0)
);

CREATE INDEX idx_aggregates_label_id ON aggregates(label_id);
CREATE INDEX idx_aggregates_year ON aggregates(year);
CREATE INDEX idx_aggregates_year_month ON aggregates(year, month);
