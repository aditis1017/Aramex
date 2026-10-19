-- Safe to re-run: alters hawb_dimensions to TEXT only when the column is not already TEXT.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'shipment'
          AND column_name = 'hawb_dimensions'
          AND udt_name <> 'text'
    ) THEN
        ALTER TABLE shipment
            ALTER COLUMN hawb_dimensions TYPE TEXT;
    END IF;
END $$;
