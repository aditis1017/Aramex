-- Safe to re-run: drops legacy indexes from an earlier draft, then adds constraints only when missing.
DROP INDEX IF EXISTS uq_account_ar_master_charge_type;
DROP INDEX IF EXISTS uq_account_ap_master_charge_type;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_account_ar_master_charge_type'
    ) THEN
        ALTER TABLE account
            ADD CONSTRAINT uq_account_ar_master_charge_type
            UNIQUE (ar_account_id, charge_type);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_account_ap_master_charge_type'
    ) THEN
        ALTER TABLE account
            ADD CONSTRAINT uq_account_ap_master_charge_type
            UNIQUE (ap_account_id, charge_type);
    END IF;
END $$;
