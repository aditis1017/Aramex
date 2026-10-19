UPDATE account
SET default_ar_account_number = 'ASL001'
WHERE entity_code IN ('RUH', 'DHA', 'JED')
  AND account_nature = 'ENTITY_SETTINGS'
  AND charge_type = 'DTA';
