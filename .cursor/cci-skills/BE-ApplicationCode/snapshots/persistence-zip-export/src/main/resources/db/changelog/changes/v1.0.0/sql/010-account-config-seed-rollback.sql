DELETE FROM account
WHERE account_nature = 'ENTITY_SETTINGS'
  AND charge_type IN ('ECD', 'DTC', 'DTA', 'ECS', 'DTS', 'EC3', 'DT3')
  AND created_by = 'System'
  AND updated_by = 'System'
  AND ar_account_id IS NULL
  AND ap_account_id IS NULL;

DROP FUNCTION IF EXISTS seed_entity_settings_accounts();
