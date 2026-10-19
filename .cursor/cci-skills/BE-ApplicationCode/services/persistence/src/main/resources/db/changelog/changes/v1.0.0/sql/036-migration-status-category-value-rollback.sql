DELETE FROM category_value
WHERE code = 'MIGRATION_STATUS';

DELETE FROM category
WHERE code = 'SYSTEM_FLAG'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv
    INNER JOIN category c ON c.id = cv.category_id
    WHERE c.code = 'SYSTEM_FLAG'
);
