-- Replace MARKET_STRUCTURE category values:
--   DIRECT   -> PAYMENT_REFERENCE (1. Payment Reference)
--   INDIRECT -> OTHERS            (2. Other)
-- Remove DEACTIVATED from ACCOUNT_STATUS (migrate existing accounts to BLOCKED)

UPDATE ap_account
SET market_structure = 'PAYMENT_REFERENCE'
WHERE UPPER(TRIM(market_structure)) = 'DIRECT';

UPDATE ap_account
SET market_structure = 'OTHERS'
WHERE UPPER(TRIM(market_structure)) = 'INDIRECT';

UPDATE category_value cv
SET is_active = false
FROM category c
WHERE cv.category_id = c.id
  AND c.code = 'MARKET_STRUCTURE'
  AND cv.code IN ('DIRECT', 'INDIRECT');

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('PAYMENT_REFERENCE', 'Payment Reference', 'Payment Reference'),
                         ('OTHERS', 'Other', 'Other')
) AS v(code, name, label)
WHERE c.code = 'MARKET_STRUCTURE'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value existing
    WHERE existing.code = v.code
);

UPDATE ap_account
SET ap_account_status = 'BLOCKED'
WHERE UPPER(TRIM(ap_account_status)) = 'DEACTIVATED';

UPDATE ar_account
SET status = 'BLOCKED'
WHERE UPPER(TRIM(status)) = 'DEACTIVATED';

DELETE FROM category_value cv
    USING category c
WHERE cv.category_id = c.id
  AND c.code = 'ACCOUNT_STATUS'
  AND cv.code = 'DEACTIVATED';
