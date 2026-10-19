-- Charge schema updates rollback (049, 048, 044, 043, 042, 041, 040)

-- ===== 049-charge-shipping-charges-rollback.sql =====
ALTER TABLE charge
    DROP COLUMN IF EXISTS shipping_charges;

-- ===== 048-ar-account-currency-fields-rollback.sql =====
ALTER TABLE ar_account
    DROP COLUMN IF EXISTS billing_currency;

ALTER TABLE ar_account
    DROP COLUMN IF EXISTS rate_currency;

-- ===== 044-rate-sheet-item-business-category-type-rollback.sql =====
ALTER TABLE rate_sheet_item
    DROP COLUMN IF EXISTS category_type;

ALTER TABLE rate_sheet_item
    DROP COLUMN IF EXISTS business_type;

-- ===== 043-charge-line-item-description-corporate-rate-rollback.sql =====
ALTER TABLE charge_line_item
    DROP COLUMN IF EXISTS contracted_rate;

ALTER TABLE charge_line_item
    DROP COLUMN IF EXISTS service_code_description;

-- ===== 042-charge-cif-currency-rollback.sql =====
ALTER TABLE charge
    DROP COLUMN IF EXISTS cif_currency;

-- ===== 041-ap-account-audit-columns-rollback.sql =====
DROP INDEX IF EXISTS idx_ap_account_updated_time;

ALTER TABLE ap_account
    DROP COLUMN IF EXISTS updated_time;

ALTER TABLE ap_account
    DROP COLUMN IF EXISTS created_time;

ALTER TABLE ap_account
    DROP COLUMN IF EXISTS updated_by;

ALTER TABLE ap_account
    DROP COLUMN IF EXISTS created_by;

-- ===== 040-charge-cif-fields-rollback.sql =====
ALTER TABLE charge
    DROP COLUMN IF EXISTS cif_currency_rate;

ALTER TABLE charge
    DROP COLUMN IF EXISTS cif_value;
