-- Rename G_BUSINESS_TYPE label and add Account Business Type global rule criteria
-- Idempotent inserts; safe to re-run

-- ============================================================
-- 1) Rename Business Type -> Shipment Business Type (G_BUSINESS_TYPE)
-- ============================================================
UPDATE category_value cv
SET label = 'Shipment Business Type'
FROM category c
WHERE cv.category_id = c.id
  AND c.code = 'GLOBAL_RULE'
  AND cv.code = 'G_BUSINESS_TYPE'
  AND cv.label = 'Business Type';

-- ============================================================
-- 2) GLOBAL_RULE criteria: Account Business Type (reuses BUSINESS_TYPE values)
-- ============================================================
INSERT INTO category_value (category_id, code, name, label, is_active, load_data_from)
SELECT c.id, v.code, v.name, v.label, true, v.load_data_from
FROM category c
         JOIN (VALUES
                   ('G_ACCOUNT_BUSINESS_TYPE', 'accountBusinessType', 'Account Business Type', 'BUSINESS_TYPE')
) v(code, name, label, load_data_from) ON TRUE
WHERE c.code = 'GLOBAL_RULE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);
