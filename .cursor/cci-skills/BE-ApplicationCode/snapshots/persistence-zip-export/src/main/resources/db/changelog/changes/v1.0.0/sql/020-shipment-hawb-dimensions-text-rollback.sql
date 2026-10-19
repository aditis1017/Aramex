-- Safe to re-run: reverts hawb_dimensions to VARCHAR(255) only when the column is TEXT.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'shipment'
          AND column_name = 'hawb_dimensions'
          AND udt_name = 'text'
    ) THEN
        ALTER TABLE shipment
            ALTER COLUMN hawb_dimensions TYPE VARCHAR(255);
    END IF;
END $$;
