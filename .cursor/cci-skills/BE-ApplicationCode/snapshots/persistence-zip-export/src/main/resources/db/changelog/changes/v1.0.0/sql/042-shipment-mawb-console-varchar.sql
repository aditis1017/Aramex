DROP VIEW IF EXISTS v_entity_charge_dashboard;

ALTER TABLE shipment
    ALTER COLUMN mawb_console TYPE VARCHAR(100)
    USING CASE
        WHEN mawb_console IS NULL THEN NULL
        ELSE mawb_console::TEXT
    END;
