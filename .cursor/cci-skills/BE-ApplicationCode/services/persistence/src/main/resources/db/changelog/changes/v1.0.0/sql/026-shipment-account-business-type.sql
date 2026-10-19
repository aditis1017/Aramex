ALTER TABLE shipment
    ADD COLUMN IF NOT EXISTS account_business_type VARCHAR(10);
