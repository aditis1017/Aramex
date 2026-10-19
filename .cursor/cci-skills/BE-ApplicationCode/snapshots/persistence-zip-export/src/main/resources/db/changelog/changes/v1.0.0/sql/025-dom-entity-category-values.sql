-- DOM_ENTITY category and RUH value for domestic shipment entity selection

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('DOM_ENTITY', 'Domestic Entity')
) v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.code = v.code
);

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('DOM_ENTITY_SA', 'RUH', 'RUH')
) v(code, name, label)
WHERE c.code = 'DOM_ENTITY'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);
