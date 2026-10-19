DELETE FROM charge_type_doc_id_mapping
WHERE charge_type IN (
    'DTC', 'DTA', 'ECD', 'DTS', 'DT3', 'ECS', 'EC3',
    'DTN', 'DAN', 'CEN', 'DSN', 'DPN', 'EON', 'ETN'
);

DELETE FROM category_value;
DELETE FROM category;
DELETE FROM global_setting WHERE target_value = 'SHIPMENT_FILTER' AND destination_code = 'SA';
DELETE FROM currency;
