DELETE FROM account
WHERE account_nature = 'AP'
  AND charge_type IS NULL
  AND ap_account_id IN (
      SELECT id
      FROM ap_account
      WHERE (entity_code IN ('RUH', 'JED', 'DHA') AND account_number = '1100053259')
         OR (entity_code NOT IN ('RUH', 'JED', 'DHA') AND account_number = 'ASL001')
  );

DELETE FROM ap_account
WHERE (entity_code IN ('RUH', 'JED', 'DHA') AND account_number = '1100053259')
   OR (entity_code NOT IN ('RUH', 'JED', 'DHA') AND account_number = 'ASL001');
