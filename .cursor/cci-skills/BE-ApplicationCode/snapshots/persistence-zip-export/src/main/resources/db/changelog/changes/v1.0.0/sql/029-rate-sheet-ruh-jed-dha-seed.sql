-- Seed rate sheets / items / rates for RUH, JED, DHA ENTITY_SETTINGS (DTC, DTA, ECD)
-- Runs only when rate_sheet_item is empty (see changelog precondition).

WITH seed (
    charge_type,
    service_code, description, shipment_type, business_type, category_type,
    rate1_type, rate1_percentage_of, rate1_value,
    rate2_type, rate2_percentage_of, rate2_value
) AS (
    VALUES
        -- DTC (DDU)
        ('DTC', 'Revenue',          'Admin Fee',        'DDU', 'B2C', 'LV',
         'FIXED', NULL, 3.75,  NULL, NULL, NULL),
        ('DTC', 'Revenue',          'Admin Fee',        'DDU', 'B2B', 'LV',
         'FIXED', NULL, 20.00, NULL, NULL, NULL),
        ('DTC', 'Revenue',          'Admin Fee',        'DDU', 'B2B', 'HV',
         'FIXED', NULL, 35.00, NULL, NULL, NULL),
        ('DTC', 'Revenue',          'Admin Fee',        'DDU', 'B2C', 'HV',
         'FIXED', NULL, 35.00, NULL, NULL, NULL),
        ('DTC', 'Disbursement Fee', 'Disbursement Fee', 'DDU', 'B2B', 'HV',
         'FIXED', NULL, 100.00, NULL, NULL, NULL),

        -- DTA (DDP)
        ('DTA', 'Revenue',          'Admin Fee',        'DDP', 'B2C', 'LV',
         'FIXED', NULL, 3.75,  NULL, NULL, NULL),
        ('DTA', 'Revenue',          'Admin Fee',        'DDP', 'B2B', 'HV',
         'FIXED', NULL, 35.00, NULL, NULL, NULL),
        ('DTA', 'Revenue',          'Admin Fee',        'DDP', 'B2C', 'HV',
         'FIXED', NULL, 35.00, NULL, NULL, NULL),
        ('DTA', 'Revenue',          'Admin Fee',        'DDP', 'B2B', 'LV',
         'FIXED', NULL, 20.00, NULL, NULL, NULL),
        ('DTA', 'Disbursement Fee', 'Disbursement Fee', 'DDP', 'B2B', 'HV',
         'FIXED', NULL, 100.00, NULL, NULL, NULL),

        -- ECD (VAT)
        ('ECD', 'VAT', 'Import VAT', 'ALL', 'B2C', 'LV',
         'PERCENTAGE', 'CIF_VALUE', 15.00, 'FIXED', NULL, 17.25)
),
target_accounts AS (
    SELECT a.id AS account_id, a.charge_type
    FROM account a
    WHERE a.account_nature = 'ENTITY_SETTINGS'
      AND a.ar_account_id IS NULL
      AND a.ap_account_id IS NULL
      AND a.entity_code IN ('RUH', 'JED', 'DHA')
      AND a.charge_type IN ('DTC', 'DTA', 'ECD')
),
created_sheets AS (
    INSERT INTO rate_sheet (
        id, created_by, updated_by, created_time, updated_time, is_active, account_id
    )
    SELECT
        gen_random_uuid(),
        'System',
        'System',
        NOW(),
        NOW(),
        TRUE,
        ta.account_id
    FROM target_accounts ta
    WHERE NOT EXISTS (
        SELECT 1 FROM rate_sheet rs WHERE rs.account_id = ta.account_id
    )
    RETURNING id AS rate_sheet_id, account_id
),
-- Combine newly created sheets with any pre-existing sheets (CTEs cannot see each other's writes)
target_sheets AS (
    SELECT
        cs.rate_sheet_id,
        ta.charge_type
    FROM created_sheets cs
    JOIN target_accounts ta ON ta.account_id = cs.account_id

    UNION ALL

    SELECT
        rs.id AS rate_sheet_id,
        ta.charge_type
    FROM target_accounts ta
    JOIN rate_sheet rs ON rs.account_id = ta.account_id
    WHERE NOT EXISTS (
        SELECT 1 FROM created_sheets cs WHERE cs.account_id = ta.account_id
    )
),
inserted_items AS (
    INSERT INTO rate_sheet_item (
        id, created_by, updated_by, created_time, updated_time,
        rate_sheet_id, description, service_code, shipment_type,
        business_type, category_type, is_deleted
    )
    SELECT
        gen_random_uuid(),
        'System',
        'System',
        NOW(),
        NOW(),
        ts.rate_sheet_id,
        s.description,
        s.service_code,
        s.shipment_type,
        s.business_type,
        s.category_type,
        FALSE
    FROM target_sheets ts
    JOIN seed s ON s.charge_type = ts.charge_type
    RETURNING
        id,
        rate_sheet_id,
        service_code,
        description,
        shipment_type,
        business_type,
        category_type
)
INSERT INTO rate_sheet_item_rate (
    id, created_by, updated_by, created_time, updated_time,
    rate_sheet_item_id, rate_type, percentage_of, value
)
SELECT
    gen_random_uuid(),
    'System',
    'System',
    NOW(),
    NOW(),
    ii.id,
    r.rate_type,
    r.percentage_of,
    r.value
FROM inserted_items ii
JOIN target_sheets ts ON ts.rate_sheet_id = ii.rate_sheet_id
JOIN seed s
  ON s.charge_type = ts.charge_type
 AND s.service_code = ii.service_code
 AND s.description = ii.description
 AND s.shipment_type = ii.shipment_type
 AND s.business_type = ii.business_type
 AND s.category_type = ii.category_type
CROSS JOIN LATERAL (
    VALUES
        (s.rate1_type, s.rate1_percentage_of, s.rate1_value),
        (s.rate2_type, s.rate2_percentage_of, s.rate2_value)
) AS r(rate_type, percentage_of, value)
WHERE r.rate_type IS NOT NULL;
