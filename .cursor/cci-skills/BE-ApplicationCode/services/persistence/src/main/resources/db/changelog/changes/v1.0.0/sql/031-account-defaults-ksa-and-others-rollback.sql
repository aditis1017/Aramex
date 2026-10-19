UPDATE account
SET default_account_number = NULL
WHERE account_nature = 'ENTITY_SETTINGS'
  AND entity_code IN ('DHA', 'RUH', 'JED')
  AND charge_type = 'ECD'
  AND default_account_number = '1100053259';

UPDATE account
SET default_ar_account_number = NULL
WHERE entity_code IN ('DHA', 'RUH', 'JED')
  AND charge_type = 'DTA'
  AND default_ar_account_number = 'ASL001';

UPDATE account
SET default_cash_account = NULL
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'JED'
  AND default_cash_account = '6005420';

UPDATE account
SET default_cash_account = NULL
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'DHA'
  AND default_cash_account = 'CASHDHA';

UPDATE account
SET default_cash_account = NULL
WHERE charge_type IN ('DTC', 'DTS', 'DT3')
  AND entity_code = 'RUH'
  AND default_cash_account = '1001';

UPDATE account
SET default_account_number = NULL
WHERE account_nature = 'ENTITY_SETTINGS'
  AND entity_code NOT IN ('DHA', 'RUH', 'JED')
  AND charge_type IN ('ECS', 'EC3')
  AND default_account_number = 'ASL001';
