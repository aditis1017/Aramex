-- Migration status flag for DDP shipment reprocessing (name=false = pending, name=true = complete)

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('MIGRATION_FLAG', 'System Flag')
) v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.code = v.code
);

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('MIGRATION_STATUS', 'false', 'Migration Status')
) v(code, name, label)
WHERE c.code = 'MIGRATION_FLAG'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);
