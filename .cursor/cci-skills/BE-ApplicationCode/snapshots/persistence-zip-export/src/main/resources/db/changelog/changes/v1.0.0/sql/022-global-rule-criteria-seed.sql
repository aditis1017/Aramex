-- GLOBAL_RULE criteria: Customs Type, Business Type, FRDM Flag
-- Idempotent inserts; safe if CUSTOMS_TYPE / BUSINESS_TYPE already seeded in 006

-- ============================================================
-- 1) FRDM_FLAG category
-- ============================================================
INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('FRDM_FLAG', 'FRDM Flag')
) v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.code = v.code
);

-- ============================================================
-- 2) FRDM_FLAG values: Yes, No
-- ============================================================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('FRDM_YES', 'Yes', 'Yes'),
                         ('FRDM_NO',  'No',  'No')
) v(code, name, label)
WHERE c.code = 'FRDM_FLAG'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- ============================================================
-- 3) Ensure CUSTOMS_TYPE values (DDP, DDU) exist
-- ============================================================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('DDP', 'DDP', 'DDP'),
                         ('DDU', 'DDU', 'DDU')
) v(code, name, label)
WHERE c.code = 'CUSTOMS_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- ============================================================
-- 4) Ensure BUSINESS_TYPE values (B2B, B2C) exist
-- ============================================================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('B2B_BT', 'B2B', 'B2B'),
                         ('B2C_BT', 'B2C', 'B2C')
) v(code, name, label)
WHERE c.code = 'BUSINESS_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- ============================================================
-- 5) GLOBAL_RULE criteria fields
-- ============================================================
INSERT INTO category_value (category_id, code, name, label, is_active, load_data_from)
SELECT c.id, v.code, v.name, v.label, true, v.load_data_from
FROM category c
         JOIN (VALUES
                   ('G_CUSTOMS_TYPE',  'shipmentType',  'Customs Type',  'CUSTOMS_TYPE'),
                   ('G_BUSINESS_TYPE', 'businessType',  'Business Type', 'BUSINESS_TYPE'),
                   ('G_FRDM_FLAG',     'frdmFlag',      'FRDM Flag',     'FRDM_FLAG')
) v(code, name, label, load_data_from) ON TRUE
WHERE c.code = 'GLOBAL_RULE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);
