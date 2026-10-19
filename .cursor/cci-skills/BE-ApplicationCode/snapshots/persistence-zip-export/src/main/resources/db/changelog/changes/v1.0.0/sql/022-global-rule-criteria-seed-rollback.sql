-- Rollback GLOBAL_RULE criteria and FRDM_FLAG reference data added in 022

DELETE FROM category_value
WHERE code IN (
    'G_CUSTOMS_TYPE',
    'G_BUSINESS_TYPE',
    'G_FRDM_FLAG',
    'FRDM_YES',
    'FRDM_NO'
);

DELETE FROM category c
WHERE c.code = 'FRDM_FLAG'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.category_id = c.id
);
