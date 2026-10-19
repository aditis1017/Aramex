-- Business type evolution rollback (051, 050, 036, 035)

-- ===== 051-ar-account-business-segment-rollback.sql =====
UPDATE ar_account
SET business_type = COALESCE(business_type, business_segment)
WHERE business_segment IS NOT NULL
  AND TRIM(business_segment) <> '';

ALTER TABLE ar_account
    DROP COLUMN IF EXISTS business_segment;

-- ===== 050-drop-account-ar-business-type-rollback.sql =====
ALTER TABLE account
    ADD COLUMN IF NOT EXISTS ar_business_type VARCHAR(100);

-- ===== 036-ar-account-business-type-rollback.sql =====
ALTER TABLE ar_account
    DROP COLUMN IF EXISTS business_type;

-- ===== 035-shipment-business-type-rollback.sql =====
ALTER TABLE shipment
    DROP COLUMN IF EXISTS shipment_business_type;
