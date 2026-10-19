-- Business type evolution (035, 036, 050, 051)
-- Consolidated migration for fresh database bootstrap

-- ===== 035-shipment-business-type.sql =====
ALTER TABLE shipment
    ADD COLUMN IF NOT EXISTS shipment_business_type VARCHAR(10);

-- ===== 036-ar-account-business-type.sql =====
ALTER TABLE ar_account
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(100);

-- ===== 050-drop-account-ar-business-type.sql =====
ALTER TABLE account
    DROP COLUMN IF EXISTS ar_business_type;

-- ===== 051-ar-account-business-segment.sql =====
ALTER TABLE ar_account
    ADD COLUMN IF NOT EXISTS business_segment VARCHAR(100);

UPDATE ar_account
SET business_segment = TRIM(business_type),
    business_type = CASE
        WHEN UPPER(TRIM(business_type)) IN ('B2B', 'B2C') THEN TRIM(business_type)
        ELSE NULL
    END
WHERE business_type IS NOT NULL
  AND TRIM(business_type) <> '';
