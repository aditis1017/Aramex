DROP VIEW IF EXISTS v_entity_charge_dashboard;

ALTER TABLE shipment
    ALTER COLUMN mawb_console TYPE BIGINT
    USING CASE
        WHEN mawb_console IS NULL OR TRIM(mawb_console) = '' THEN NULL
        ELSE NULLIF(REGEXP_REPLACE(mawb_console, '[^0-9]', '', 'g'), '')::BIGINT
    END;

CREATE VIEW v_entity_charge_dashboard AS
SELECT
    c.id AS charge_id,
    c.created_time AS created_time,
    c.charge_code AS chargetype,
    c.status AS chargestatus,
    c.currency AS currency,
    c.outstanding_amount AS outstanding_amount,
    COALESCE(NULLIF(c.actual_amount, 0), c.estimate_amount) AS actual_amount,
    e.code AS entity,
    s.hawb_number AS trackingnumber,
    s.origin_entity AS originentity,
    s.hawb_destination_entity AS destinationentity,
    s.third_party_entity AS thirdpartentity,
    s.third_party_country AS thirdpartcountry,
    s.shipment_business_type AS businesstype,
    s.category_type AS category,
    s.shipper_country_code AS origincountry,
    s.consignee_country_code AS destinationcountry,
    s.mawb_console AS console,
    s.pickup_date AS pickupdate,
    s.hawb_alerted_date AS alerteddate,
    c.account_type AS account_type,
    s.consignee_number AS consignee_number,
    s.shipper_number AS shipper_number,
    s.third_party_number AS third_party_number,
    CAST(
            CASE
                WHEN upper(c.charge_code) IN ('ECD', 'ECS', 'EC3') THEN first_cli.ap_account_number
                WHEN upper(c.charge_code) IN ('DTC', 'DT3', 'DTS','DTA') THEN
                    CASE
                        WHEN a.account_nature = 'ENTITY_SETTINGS' THEN NULL
                        ELSE ar.account_number
                        END
                ELSE s.consignee_number
                END AS VARCHAR(250)
    ) AS account_number,
    CAST(
            CASE
                WHEN COALESCE(TRIM(
                                      CASE
                                          WHEN upper(c.charge_code) IN ('ECD', 'ECS', 'EC3') THEN first_cli.ap_account_number
                                          WHEN upper(c.charge_code) IN ('DTC', 'DT3', 'DTS','DTA') THEN
                                              CASE
                                                  WHEN a.account_nature = 'ENTITY_SETTINGS' THEN NULL
                                                  ELSE ar.account_number
                                                  END
                                          ELSE s.consignee_number
                                          END
                              ), '') = ''
                    THEN NULL
                WHEN upper(c.charge_code) IN ('ECD', 'ECS', 'EC3') THEN ap.account_name
                WHEN upper(c.charge_code) IN ('DTC', 'DT3', 'DTS','DTA') THEN
                    CASE
                        WHEN a.account_nature = 'ENTITY_SETTINGS' THEN NULL
                        ELSE ar.account_name
                        END
                ELSE s.consignee_name
                END AS VARCHAR(250)
    ) AS account_name,
    CAST(NULL AS VARCHAR(500)) AS declarationnumber
FROM charge c
         LEFT JOIN entity e ON e.id = c.entity_id
         LEFT JOIN shipment s ON s.id = c.shipment_id
         LEFT JOIN LATERAL (
    SELECT cli.account_id, cli.ap_account_number
    FROM charge_line_item cli
    WHERE cli.charge_id = c.id
      AND cli.account_id IS NOT NULL
    ORDER BY cli.created_time ASC NULLS LAST, cli.id ASC
        LIMIT 1
) first_cli ON TRUE
         LEFT JOIN account a ON a.id = first_cli.account_id
         LEFT JOIN ar_account ar ON ar.id = a.ar_account_id
         LEFT JOIN ap_account ap
                   ON lower(ap.account_number) = lower(first_cli.ap_account_number)
                       AND lower(ap.entity_code) = lower(e.code);
