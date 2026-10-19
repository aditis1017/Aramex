-- Service charge mapping seed (formerly 025-service-charge-mapping-seed)
-- Must run after 008-service-codes-seed so service_codes rows exist for the joins.

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
        ('Customs Duty Inbound', 'DTA', 'Customs Duty Outbound', 'ECS'),
        ('Other Customs Charges', 'DTA', 'Other Customs Charges', 'ECS'),
        ('VAT', 'DTA', 'VAT', 'ECS'),
        ('Transit Charges', 'DTA', 'Transit Charges', 'ECS'),
        ('Other Charges', 'DTA', 'Other Charges', 'ECS'),
        ('Cost', 'DTA', 'Cost', 'ECS'),
        ('absorb', 'DTA', 'absorb', 'ECS'),

        ('Customs Duty Inbound', 'DTA', 'Customs Duty Outbound', 'EC3'),
        ('Other Customs Charges', 'DTA', 'Other Customs Charges', 'EC3'),
        ('VAT', 'DTA', 'VAT', 'EC3'),
        ('Transit Charges', 'DTA', 'Transit Charges', 'EC3'),
        ('Other Charges', 'DTA', 'Other Charges', 'EC3'),
        ('Cost', 'DTA', 'Cost', 'EC3'),
        ('absorb', 'DTA', 'absorb', 'EC3')
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
