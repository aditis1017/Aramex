-- Charge dashboard indexes and view (037, 038, 039)
-- Consolidated migration for fresh database bootstrap

-- ===== 037-charge-account-id-and-indexes.sql =====
-- Dashboard / filter indexes on charge (no denormalized account_id).
CREATE INDEX IF NOT EXISTS idx_charge_charge_code ON charge (charge_code);
CREATE INDEX IF NOT EXISTS idx_charge_charge_code_created_time ON charge (charge_code, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_charge_shipment_id ON charge (shipment_id);
CREATE INDEX IF NOT EXISTS idx_charge_entity_id ON charge (entity_id);

-- ===== 038-entity-charge-dashboard-view.sql =====
-- Support default dashboard sort without charge_code filter.
CREATE INDEX IF NOT EXISTS idx_charge_created_time ON charge (created_time DESC);

-- Speed distinct-account lateral lookup from line items.
CREATE INDEX IF NOT EXISTS idx_charge_line_item_charge_account
    ON charge_line_item (charge_id, account_id)
    WHERE account_id IS NOT NULL;

-- ===== 039-entity-charge-dashboard-view-account-resolve.sql =====
-- Align dashboard view account_number/account_name with legacy search() rules:
-- ECD/ECS/EC3 → account.default_account_number + ap_account.account_name
-- DTA         → ar_account
-- DTS         → shipment shipper
-- DT3         → shipment shipper (same as DTS)
-- DTC         → charge line account: blank for ENTITY_SETTINGS, else ar_account
-- other       → shipment consignee
--
DROP VIEW IF EXISTS v_entity_charge_dashboard;

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
                WHEN upper(c.charge_code) IN ('ECD', 'ECS', 'EC3') THEN a.default_account_number
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
                                          WHEN upper(c.charge_code) IN ('ECD', 'ECS', 'EC3') THEN a.default_account_number
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
    CAST(
            (
                SELECT STRING_AGG(DISTINCT cli.declaration_number, ',')
                FROM charge_line_item cli
                WHERE cli.charge_id = c.id
                  AND cli.payment_reference IS NOT NULL
            ) AS VARCHAR(500)
    ) AS declarationnumber
FROM charge c
         LEFT JOIN entity e ON e.id = c.entity_id
         LEFT JOIN shipment s ON s.id = c.shipment_id
         LEFT JOIN LATERAL (
    SELECT cli.account_id
    FROM charge_line_item cli
    WHERE cli.charge_id = c.id
      AND cli.account_id IS NOT NULL
    ORDER BY cli.created_time ASC NULLS LAST, cli.id ASC
        LIMIT 1
) first_cli ON TRUE
LEFT JOIN account a ON a.id = first_cli.account_id
LEFT JOIN ar_account ar ON ar.id = a.ar_account_id
LEFT JOIN ap_account ap
    ON lower(ap.account_number) = lower(a.default_account_number)
   AND lower(ap.entity_code) = lower(e.code);

-- Support case-insensitive AP join used by the view (chargeType filter always present → charge indexes already cover main path).
CREATE INDEX IF NOT EXISTS idx_ap_account_number_entity_lower
    ON ap_account (lower(account_number), lower(entity_code));

CREATE INDEX IF NOT EXISTS idx_account_default_account_number_lower
    ON account (lower(default_account_number));

CREATE INDEX IF NOT EXISTS idx_charge_line_item_declaration_number
    ON charge_line_item (declaration_number)
    WHERE declaration_number IS NOT NULL;
