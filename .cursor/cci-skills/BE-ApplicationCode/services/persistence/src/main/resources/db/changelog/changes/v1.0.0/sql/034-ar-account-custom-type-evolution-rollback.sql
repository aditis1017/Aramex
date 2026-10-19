ALTER TABLE account
    ADD COLUMN IF NOT EXISTS ar_custom_type VARCHAR(50);

UPDATE account a
SET ar_custom_type = aa.ar_custom_type,
    updated_time = COALESCE(a.updated_time, NOW())
FROM ar_account aa
WHERE a.ar_account_id = aa.id
  AND a.account_nature = 'AR'
  AND aa.ar_custom_type IS NOT NULL
  AND TRIM(aa.ar_custom_type) <> ''
  AND UPPER(TRIM(COALESCE(a.charge_type, ''))) = 'DTC';

ALTER TABLE ar_account
    DROP COLUMN IF EXISTS ar_custom_type;
