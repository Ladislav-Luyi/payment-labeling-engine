-- Flyway migration V2: Add helper views and functions for reporting

-- View for payment summary with labels
CREATE VIEW v_payment_summary AS
SELECT 
    p.id,
    p.payment_date,
    p.amount,
    p.currency,
    p.reference,
    p.transaction_type,
    p.counterparty_name,
    p.receiver_info,
    '' AS labels,
    p.created_at
FROM payments p
ORDER BY p.payment_date DESC;

-- View for aggregate summary with label names
CREATE VIEW v_aggregate_summary AS
SELECT 
    a.id,
    l.name AS label_name,
    a."year",
    a."month",
    a.total_amount,
    a.transaction_count,
    CAST(a."year" AS VARCHAR) || '-' || LPAD(CAST(a."month" AS VARCHAR), 2, '0') AS period
FROM aggregates a
JOIN labels l ON a.label_id = l.id
ORDER BY a."year" DESC, a."month" DESC, l.name;
