-- Account configuration seed (entity settings)

CREATE OR REPLACE FUNCTION seed_entity_settings_accounts()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    inserted_count INTEGER;
BEGIN
    INSERT INTO account (
        id,
        created_by,
        updated_by,
        created_time,
        updated_time,
        entity_code,
        account_nature,
        charge_type
    )
    SELECT
        gen_random_uuid(),
        'System',
        'System',
        NOW(),
        NOW(),
        e.code,
        'ENTITY_SETTINGS',
        ct.charge_type
    FROM entity e
    CROSS JOIN (
        VALUES
            ('ECD'),
            ('DTC'),
            ('DTA'),
            ('ECS'),
            ('DTS'),
            ('EC3'),
            ('DT3')
    ) AS ct(charge_type)
    WHERE e.is_active = TRUE
      AND NOT EXISTS (
          SELECT 1
          FROM account a
          WHERE a.entity_code = e.code
            AND a.account_nature = 'ENTITY_SETTINGS'
            AND a.charge_type = ct.charge_type
      );

    GET DIAGNOSTICS inserted_count = ROW_COUNT;
    RETURN inserted_count;
END;
$$;


INSERT INTO account (
    id,
    created_by,
    updated_by,
    created_time,
    updated_time,
    entity_code,
    account_nature,
    charge_type,
    version
)
SELECT
    gen_random_uuid(),
    'System',
    'System',
    NOW(),
    NOW(),
    e.code,
    'ENTITY_SETTINGS',
    ct.charge_type,
    0
FROM entity e
CROSS JOIN (
    VALUES
        ('ECD'),
        ('DTC'),
        ('DTA'),
        ('ECS'),
        ('DTS'),
        ('EC3'),
        ('DT3')
) AS ct(charge_type)
WHERE e.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM account a
      WHERE a.entity_code = e.code
        AND a.account_nature = 'ENTITY_SETTINGS'
        AND a.charge_type = ct.charge_type
  );
