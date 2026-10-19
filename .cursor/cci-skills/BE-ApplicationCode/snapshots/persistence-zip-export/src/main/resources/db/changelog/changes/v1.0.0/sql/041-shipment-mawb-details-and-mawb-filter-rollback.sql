DELETE FROM global_setting WHERE target_value = 'MAWB_FILTER' AND destination_code = 'SA';
DELETE FROM category_value WHERE code = 'MAWB_FILTER';
DROP TABLE IF EXISTS shipment_mawb_details;
