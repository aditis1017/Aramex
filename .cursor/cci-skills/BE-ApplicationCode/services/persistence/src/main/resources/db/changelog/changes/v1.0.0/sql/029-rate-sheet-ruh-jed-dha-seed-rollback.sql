DELETE FROM rate_sheet_item_rate
WHERE rate_sheet_item_id IN (
    SELECT rsi.id
    FROM rate_sheet_item rsi
    JOIN rate_sheet rs ON rs.id = rsi.rate_sheet_id
    JOIN account a ON a.id = rs.account_id
    WHERE a.account_nature = 'ENTITY_SETTINGS'
      AND a.entity_code IN ('RUH', 'JED', 'DHA')
      AND a.charge_type IN ('DTC', 'DTA', 'ECD')
);

DELETE FROM rate_sheet_item
WHERE rate_sheet_id IN (
    SELECT rs.id
    FROM rate_sheet rs
    JOIN account a ON a.id = rs.account_id
    WHERE a.account_nature = 'ENTITY_SETTINGS'
      AND a.entity_code IN ('RUH', 'JED', 'DHA')
      AND a.charge_type IN ('DTC', 'DTA', 'ECD')
);

DELETE FROM rate_sheet
WHERE account_id IN (
    SELECT a.id
    FROM account a
    WHERE a.account_nature = 'ENTITY_SETTINGS'
      AND a.entity_code IN ('RUH', 'JED', 'DHA')
      AND a.charge_type IN ('DTC', 'DTA', 'ECD')
);
