-- Charge dashboard rollback (039, 038, 037)

-- ===== 039-entity-charge-dashboard-view-account-resolve-rollback.sql =====
DROP INDEX IF EXISTS idx_account_default_account_number_lower;
DROP INDEX IF EXISTS idx_ap_account_number_entity_lower;
DROP VIEW IF EXISTS v_entity_charge_dashboard;

-- ===== 038-entity-charge-dashboard-view-rollback.sql =====
DROP INDEX IF EXISTS idx_charge_line_item_charge_account;
DROP INDEX IF EXISTS idx_charge_created_time;

-- ===== 037-charge-account-id-and-indexes-rollback.sql =====
DROP INDEX IF EXISTS idx_charge_entity_id;
DROP INDEX IF EXISTS idx_charge_shipment_id;
DROP INDEX IF EXISTS idx_charge_charge_code_created_time;
DROP INDEX IF EXISTS idx_charge_charge_code;
