DELETE FROM service_charge_mapping
WHERE created_by = 'SYSTEM'
  AND from_entity = 'Global'
  AND to_entity = 'Global'
  AND charge_type IN ('ECS', 'EC3')
  AND from_service_code IN (
      SELECT id FROM service_codes
      WHERE service_code IN (
          'Customs Duty Inbound',
          'Other Customs Charges',
          'VAT',
          'Transit Charges',
          'Other Charges',
          'Cost',
          'absorb'
      )
        AND charge_type = 'DTA'
  );
