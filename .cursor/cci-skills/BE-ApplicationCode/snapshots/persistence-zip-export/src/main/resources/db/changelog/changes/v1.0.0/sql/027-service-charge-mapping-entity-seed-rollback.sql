DELETE FROM service_charge_mapping
WHERE created_by = 'SYSTEM'
  AND from_entity IN ('RUH', 'DHA', 'JED')
  AND charge_type IN ('ECS', 'EC3');
