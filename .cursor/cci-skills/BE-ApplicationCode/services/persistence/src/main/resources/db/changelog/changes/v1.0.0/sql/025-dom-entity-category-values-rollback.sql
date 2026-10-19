-- Rollback DOM_ENTITY reference data added in 025

DELETE FROM category_value
WHERE code = 'DOM_ENTITY_SA';

DELETE FROM category c
WHERE c.code = 'DOM_ENTITY'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.category_id = c.id
);
