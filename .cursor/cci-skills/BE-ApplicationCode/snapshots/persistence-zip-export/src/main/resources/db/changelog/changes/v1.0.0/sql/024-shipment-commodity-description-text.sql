-- Safe to re-run: alters commodity_description to TEXT only when the column is not already TEXT.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'shipment'
          AND column_name = 'commodity_description'
          AND udt_name <> 'text'
    ) THEN
        ALTER TABLE shipment
            ALTER COLUMN commodity_description TYPE TEXT;
    END IF;
END $$;
