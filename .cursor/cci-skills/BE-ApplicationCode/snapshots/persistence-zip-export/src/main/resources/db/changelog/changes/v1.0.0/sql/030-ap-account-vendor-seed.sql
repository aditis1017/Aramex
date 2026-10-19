-- AP vendor account seed
-- Runs only when ap_account is empty (see changelog precondition).

-- RUH, JED, DHA: KSA Customs Authorities
INSERT INTO ap_account (
    id,
    entity_code,
    account_name,
    account_number,
    market_structure,
    ap_account_status,
    account_type,
    created_by,
    updated_by,
    created_time,
    updated_time
)
SELECT
    gen_random_uuid(),
    e.code,
    'KSA Customs Authorities',
    '1100053259',
    'PAYMENT_REFERENCE',
    'ACTIVATED',
    'Vendor',
    'System',
    'System',
    NOW(),
    NOW()
FROM entity e
WHERE e.code IN ('RUH', 'JED', 'DHA')
  AND NOT EXISTS (
      SELECT 1
      FROM ap_account ap
      WHERE ap.entity_code = e.code
        AND ap.account_number = '1100053259'
  );

-- Account info for RUH, JED, DHA: minimum config, charge_type null
INSERT INTO account (
    id,
    created_by,
    updated_by,
    created_time,
    updated_time,
    entity_code,
    account_nature,
    ap_account_id,
    charge_type,
    version
)
SELECT
    gen_random_uuid(),
    'System',
    'System',
    NOW(),
    NOW(),
    ap.entity_code,
    'AP',
    ap.id,
    NULL,
    0
FROM ap_account ap
WHERE ap.entity_code IN ('RUH', 'JED', 'DHA')
  AND ap.account_number = '1100053259'
  AND NOT EXISTS (
      SELECT 1
      FROM account a
      WHERE a.ap_account_id = ap.id
        AND a.account_nature = 'AP'
        AND a.charge_type IS NULL
  );

-- Every other entity: Aramex Special Logistics LLC
INSERT INTO ap_account (
    id,
    entity_code,
    account_name,
    account_number,
    market_structure,
    ap_account_status,
    account_type,
    created_by,
    updated_by,
    created_time,
    updated_time
)
SELECT
    gen_random_uuid(),
    e.code,
    'Aramex Special Logistics LLC',
    'ASL001',
    'PAYMENT_REFERENCE',
    'ACTIVATED',
    'Vendor',
    'System',
    'System',
    NOW(),
    NOW()
FROM entity e
WHERE e.code NOT IN ('RUH', 'JED', 'DHA')
  AND NOT EXISTS (
      SELECT 1
      FROM ap_account ap
      WHERE ap.entity_code = e.code
        AND ap.account_number = 'ASL001'
  );

-- Account info for those other entities: minimum config, charge_type null
INSERT INTO account (
    id,
    created_by,
    updated_by,
    created_time,
    updated_time,
    entity_code,
    account_nature,
    ap_account_id,
    charge_type,
    version
)
SELECT
    gen_random_uuid(),
    'System',
    'System',
    NOW(),
    NOW(),
    ap.entity_code,
    'AP',
    ap.id,
    NULL,
    0
FROM ap_account ap
WHERE ap.account_number = 'ASL001'
  AND ap.entity_code NOT IN ('RUH', 'JED', 'DHA')
  AND NOT EXISTS (
      SELECT 1
      FROM account a
      WHERE a.ap_account_id = ap.id
        AND a.account_nature = 'AP'
        AND a.charge_type IS NULL
  );
