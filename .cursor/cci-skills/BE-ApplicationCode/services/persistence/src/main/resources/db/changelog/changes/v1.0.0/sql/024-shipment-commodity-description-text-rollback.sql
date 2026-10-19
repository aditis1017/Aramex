-- Safe to re-run: reverts commodity_description to VARCHAR(500) only when the column is TEXT.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'shipment'
          AND column_name = 'commodity_description'
          AND udt_name = 'text'
    ) THEN
        ALTER TABLE shipment
            ALTER COLUMN commodity_description TYPE VARCHAR(500);
    END IF;
END $$;
