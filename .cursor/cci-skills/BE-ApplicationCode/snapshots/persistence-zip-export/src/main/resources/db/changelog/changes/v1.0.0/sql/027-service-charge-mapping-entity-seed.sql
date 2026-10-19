-- Entity service charge mapping seed (RUH/DHA/JED DTA -> other-entity ECS/EC3)
-- Runs only when service_charge_mapping is empty (see changelog precondition).

WITH from_entities AS (
    SELECT unnest(ARRAY['RUH', 'DHA', 'JED']::text[]) AS entity_code
),
to_entities AS (
    SELECT DISTINCT sc.entity_code
    FROM service_codes sc
    WHERE sc.entity_code NOT IN ('RUH', 'DHA', 'JED')
),
service_code_relationships (
    charge_type,
    from_service_code,
    to_service_code
) AS (
    VALUES
        ('ECS', 'Customs Duty Inbound',  'Customs Duty Outbound'),
        ('ECS', 'Disbursement Fee',      'Disbursement Fee'),
        ('ECS', 'Other Charges',         'Other Charges'),
        ('ECS', 'Other Customs Charges', 'Other Customs Charges'),
        ('ECS', 'Revenue',               'Cost'),
        ('ECS', 'Transit Charges',       'Transit Charges'),
        ('ECS', 'VAT',                   'Customs Duty Outbound'),
        ('EC3', 'Customs Duty Inbound',  'Customs Duty 3rd party'),
        ('EC3', 'Disbursement Fee',      'Disbursement Fee'),
        ('EC3', 'Other Charges',         'Other Charges'),
        ('EC3', 'Other Customs Charges', 'Other Customs Charges'),
        ('EC3', 'Revenue',               'Cost'),
        ('EC3', 'Transit Charges',       'Transit Charges'),
        ('EC3', 'VAT',                   'Customs Duty 3rd party')
),
from_entity_service_codes AS (
    SELECT
        sc.id,
        sc.entity_code,
        sc.service_code
    FROM service_codes sc
    WHERE sc.entity_code IN ('RUH', 'DHA', 'JED')
      AND sc.charge_type = 'DTA'
      AND COALESCE(sc.is_deleted, false) = false
),
to_entity_service_codes AS (
    SELECT
        sc.id,
        sc.entity_code,
        sc.service_code,
        sc.charge_type
    FROM service_codes sc
    WHERE sc.entity_code NOT IN ('RUH', 'DHA', 'JED')
      AND sc.charge_type IN ('ECS', 'EC3')
      AND COALESCE(sc.is_deleted, false) = false
),
candidate_rows AS (
    SELECT
        src.id         AS from_service_code,
        tgt.id         AS to_service_code,
        fe.entity_code AS from_entity,
        te.entity_code AS to_entity,
        rel.charge_type
    FROM from_entities fe
    CROSS JOIN to_entities te
    CROSS JOIN service_code_relationships rel
    JOIN from_entity_service_codes src
        ON src.entity_code = fe.entity_code
       AND src.service_code = rel.from_service_code
    JOIN to_entity_service_codes tgt
        ON tgt.entity_code = te.entity_code
       AND tgt.service_code = rel.to_service_code
       AND tgt.charge_type = rel.charge_type
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
    cr.from_service_code,
    cr.to_service_code,
    cr.from_entity,
    cr.to_entity,
    cr.charge_type
FROM candidate_rows cr
WHERE NOT EXISTS (
    SELECT 1
    FROM service_charge_mapping existing
    WHERE existing.from_service_code = cr.from_service_code
      AND existing.to_service_code   = cr.to_service_code
      AND existing.from_entity       = cr.from_entity
      AND existing.to_entity         = cr.to_entity
      AND existing.charge_type       = cr.charge_type
);
