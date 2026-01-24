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
    STRING_AGG(DISTINCT l.name, ', ') AS labels,
    p.created_at
FROM payments p
LEFT JOIN payment_labels pl ON p.id = pl.payment_id
LEFT JOIN labels l ON pl.label_id = l.id
GROUP BY p.id, p.payment_date, p.amount, p.currency, p.reference, 
         p.transaction_type, p.counterparty_name, p.receiver_info, p.created_at
ORDER BY p.payment_date DESC;

-- View for aggregate summary with label names
CREATE VIEW v_aggregate_summary AS
SELECT 
    a.id,
    l.name AS label_name,
    a.year,
    a.month,
    a.total_amount,
    a.transaction_count,
    TO_CHAR(TO_DATE(a.year || '-' || LPAD(a.month::TEXT, 2, '0'), 'YYYY-MM'), 'YYYY-MM') AS period
FROM aggregates a
JOIN labels l ON a.label_id = l.id
ORDER BY a.year DESC, a.month DESC, l.name;

-- Function to update aggregates for a given payment
CREATE OR REPLACE FUNCTION update_aggregates_for_payment(p_payment_id BIGINT)
RETURNS void AS $$
BEGIN
    -- Delete existing aggregates for this payment's labels
    DELETE FROM aggregates
    WHERE label_id IN (
        SELECT DISTINCT label_id FROM payment_labels WHERE payment_id = p_payment_id
    )
    AND EXTRACT(YEAR FROM DATE(CONCAT(year, '-', LPAD(month::TEXT, 2, '0'), '-01'))) = EXTRACT(YEAR FROM (SELECT payment_date FROM payments WHERE id = p_payment_id))
    AND month = EXTRACT(MONTH FROM (SELECT payment_date FROM payments WHERE id = p_payment_id));

    -- Insert or update aggregates
    INSERT INTO aggregates (label_id, year, month, total_amount, transaction_count, created_at, updated_at)
    SELECT 
        pl.label_id,
        EXTRACT(YEAR FROM p.payment_date)::INTEGER,
        EXTRACT(MONTH FROM p.payment_date)::INTEGER,
        SUM(p.amount),
        COUNT(DISTINCT p.id),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM payments p
    JOIN payment_labels pl ON p.id = pl.payment_id
    WHERE pl.label_id IN (
        SELECT DISTINCT label_id FROM payment_labels WHERE payment_id = p_payment_id
    )
    AND EXTRACT(YEAR FROM p.payment_date) = EXTRACT(YEAR FROM (SELECT payment_date FROM payments WHERE id = p_payment_id))
    AND EXTRACT(MONTH FROM p.payment_date) = EXTRACT(MONTH FROM (SELECT payment_date FROM payments WHERE id = p_payment_id))
    GROUP BY pl.label_id, EXTRACT(YEAR FROM p.payment_date), EXTRACT(MONTH FROM p.payment_date)
    ON CONFLICT (label_id, year, month) DO UPDATE
    SET total_amount = EXCLUDED.total_amount,
        transaction_count = EXCLUDED.transaction_count,
        updated_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update aggregates when payment labels change
CREATE OR REPLACE FUNCTION trg_update_aggregates_on_label_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        PERFORM update_aggregates_for_payment(NEW.payment_id);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        PERFORM update_aggregates_for_payment(OLD.payment_id);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_labels_update
AFTER INSERT OR UPDATE OR DELETE ON payment_labels
FOR EACH ROW
EXECUTE FUNCTION trg_update_aggregates_on_label_change();
