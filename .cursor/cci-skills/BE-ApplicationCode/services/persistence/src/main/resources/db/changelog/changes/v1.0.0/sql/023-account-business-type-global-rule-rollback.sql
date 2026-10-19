-- Rollback Account Business Type global rule criteria added in 023

DELETE FROM category_value
WHERE code = 'G_ACCOUNT_BUSINESS_TYPE';

UPDATE category_value cv
SET label = 'Business Type'
FROM category c
WHERE cv.category_id = c.id
  AND c.code = 'GLOBAL_RULE'
  AND cv.code = 'G_BUSINESS_TYPE'
  AND cv.label = 'Shipment Business Type';
