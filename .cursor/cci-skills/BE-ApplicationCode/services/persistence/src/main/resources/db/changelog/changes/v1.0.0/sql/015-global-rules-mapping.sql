-- Global rules and service charge mapping (045, 047)
-- Consolidated migration for fresh database bootstrap

-- ===== 045-global-rule-criteria-currency.sql =====
ALTER TABLE global_rule_criteria
    ADD COLUMN IF NOT EXISTS currency VARCHAR(50);

-- ===== 047-service-charge-mapping-disbursement-fee.sql =====
WITH unique_service_codes AS (
    SELECT DISTINCT ON (service_code, charge_type)
           id,
           service_code,
           charge_type
    FROM service_codes
    ORDER BY service_code, charge_type, entity_code
),
mappings (
    from_service_code,
    from_charge_type,
    to_service_code,
    charge_type
) AS (
    VALUES
        ('Disbursement Fee', 'DTA', 'Disbursement Fee', 'ECS'),
        ('Disbursement Fee', 'DTA', 'Disbursement Fee', 'EC3')
)
INSERT INTO service_charge_mapping
(
    id,
    created_by,
    updated_by,
    created_time,
    updated_time,
    from_service_code,
    to_service_code,
    from_entity,
    to_entity,
    charge_type
)
SELECT
    gen_random_uuid(),
    'SYSTEM',
    'SYSTEM',
    NOW(),
    NOW(),
    src.id,
    tgt.id,
    'Global',
    'Global',
    m.charge_type
FROM mappings m
JOIN unique_service_codes src
    ON src.service_code = m.from_service_code
   AND src.charge_type = m.from_charge_type
JOIN unique_service_codes tgt
    ON tgt.service_code = m.to_service_code
   AND tgt.charge_type = m.charge_type
WHERE NOT EXISTS (
    SELECT 1
    FROM service_charge_mapping existing
    WHERE existing.from_service_code = src.id
      AND existing.to_service_code = tgt.id
      AND existing.from_entity = 'Global'
      AND existing.to_entity = 'Global'
      AND existing.charge_type = m.charge_type
);
