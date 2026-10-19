-- Charge and account schema updates (040, 041, 042, 043, 044, 048, 049)
-- Consolidated migration for fresh database bootstrap

-- ===== 040-charge-cif-fields.sql =====
ALTER TABLE charge
    ADD COLUMN IF NOT EXISTS cif_value DOUBLE PRECISION;

ALTER TABLE charge
    ADD COLUMN IF NOT EXISTS cif_currency_rate DOUBLE PRECISION;

-- ===== 041-ap-account-audit-columns.sql =====
ALTER TABLE ap_account
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

ALTER TABLE ap_account
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE ap_account
    ADD COLUMN IF NOT EXISTS created_time TIMESTAMPTZ;

ALTER TABLE ap_account
    ADD COLUMN IF NOT EXISTS updated_time TIMESTAMPTZ;

UPDATE ap_account
SET created_time = COALESCE(created_time, NOW()),
    updated_time = COALESCE(updated_time, created_time, NOW())
WHERE created_time IS NULL
   OR updated_time IS NULL;

ALTER TABLE ap_account
    ALTER COLUMN created_time SET NOT NULL;

ALTER TABLE ap_account
    ALTER COLUMN updated_time SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ap_account_updated_time
    ON ap_account (updated_time DESC);

-- ===== 042-charge-cif-currency.sql =====
ALTER TABLE charge
    ADD COLUMN IF NOT EXISTS cif_currency VARCHAR(50);

-- ===== 043-charge-line-item-description-corporate-rate.sql =====
ALTER TABLE charge_line_item
    ADD COLUMN IF NOT EXISTS service_code_description VARCHAR(255);

ALTER TABLE charge_line_item
    ADD COLUMN IF NOT EXISTS contracted_rate NUMERIC(19, 4) DEFAULT 0;

-- ===== 044-rate-sheet-item-business-category-type.sql =====
ALTER TABLE rate_sheet_item
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(50);

ALTER TABLE rate_sheet_item
    ADD COLUMN IF NOT EXISTS category_type VARCHAR(50);

-- ===== 048-ar-account-currency-fields.sql =====
ALTER TABLE ar_account
    ADD COLUMN IF NOT EXISTS rate_currency VARCHAR(100);

ALTER TABLE ar_account
    ADD COLUMN IF NOT EXISTS billing_currency VARCHAR(100);

-- ===== 049-charge-shipping-charges.sql =====
ALTER TABLE charge
    ADD COLUMN IF NOT EXISTS shipping_charges DOUBLE PRECISION;
