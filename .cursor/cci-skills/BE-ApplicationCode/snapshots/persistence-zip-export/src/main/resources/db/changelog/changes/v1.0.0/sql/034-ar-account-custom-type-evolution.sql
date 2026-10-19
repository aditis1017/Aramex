-- Move ar_custom_type from account (per charge config) to ar_account (AR master).

ALTER TABLE ar_account
    ADD COLUMN IF NOT EXISTS ar_custom_type VARCHAR(50);

UPDATE ar_account aa
SET ar_custom_type = src.ar_custom_type
FROM (
    SELECT DISTINCT ON (a.ar_account_id)
           a.ar_account_id,
           TRIM(a.ar_custom_type) AS ar_custom_type
    FROM account a
    WHERE a.account_nature = 'AR'
      AND a.ar_account_id IS NOT NULL
      AND a.ar_custom_type IS NOT NULL
      AND TRIM(a.ar_custom_type) <> ''
    ORDER BY a.ar_account_id,
             CASE WHEN UPPER(TRIM(a.charge_type)) = 'DTC' THEN 0 ELSE 1 END,
             a.updated_time DESC NULLS LAST
) src
WHERE aa.id = src.ar_account_id;

ALTER TABLE account
    DROP COLUMN IF EXISTS ar_custom_type;
