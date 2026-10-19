-- Account default field updates for KSA entities and other entities

UPDATE account
SET default_account_number = '1100053259'
WHERE account_nature = 'ENTITY_SETTINGS'
  AND entity_code IN ('DHA', 'RUH', 'JED')
  AND charge_type = 'ECD';

UPDATE account
SET default_ar_account_number = 'ASL001'
WHERE entity_code IN ('DHA', 'RUH', 'JED')
  AND charge_type = 'DTA';

UPDATE account
SET default_cash_account = '6005420'
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'JED';

UPDATE account
SET default_cash_account = 'CASHDHA'
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'DHA';

UPDATE account
SET default_cash_account = '1001'
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'RUH';

UPDATE account
SET default_account_number = 'ASL001'
WHERE account_nature = 'ENTITY_SETTINGS'
  AND entity_code NOT IN ('DHA', 'RUH', 'JED')
  AND charge_type IN ('ECS', 'EC3');
