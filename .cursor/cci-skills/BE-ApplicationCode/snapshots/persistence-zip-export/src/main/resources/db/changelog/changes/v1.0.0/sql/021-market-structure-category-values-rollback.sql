-- Rollback MARKET_STRUCTURE category value rename and restore DEACTIVATED in ACCOUNT_STATUS

UPDATE ap_account
SET market_structure = 'DIRECT'
WHERE UPPER(TRIM(market_structure)) = 'PAYMENT_REFERENCE';

UPDATE ap_account
SET market_structure = 'INDIRECT'
WHERE UPPER(TRIM(market_structure)) = 'OTHERS';

DELETE FROM category_value cv
    USING category c
WHERE cv.category_id = c.id
  AND c.code = 'MARKET_STRUCTURE'
  AND cv.code IN ('PAYMENT_REFERENCE', 'OTHERS');

UPDATE category_value cv
SET is_active = true
FROM category c
WHERE cv.category_id = c.id
  AND c.code = 'MARKET_STRUCTURE'
  AND cv.code IN ('DIRECT', 'INDIRECT');

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, 'DEACTIVATED', 'Deactivated', 'Deactivated', true
FROM category c
WHERE c.code = 'ACCOUNT_STATUS'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value existing
    WHERE existing.code = 'DEACTIVATED'
);
