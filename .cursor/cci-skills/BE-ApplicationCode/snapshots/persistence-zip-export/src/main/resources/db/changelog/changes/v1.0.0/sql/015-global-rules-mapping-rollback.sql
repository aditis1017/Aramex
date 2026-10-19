-- Global rules and mapping rollback (047, 045)

-- ===== 047-service-charge-mapping-disbursement-fee-rollback.sql =====
DELETE FROM service_charge_mapping m
USING service_codes src, service_codes tgt
WHERE m.from_service_code = src.id
  AND m.to_service_code = tgt.id
  AND src.service_code = 'Disbursement Fee'
  AND src.charge_type = 'DTA'
  AND tgt.service_code = 'Disbursement Fee'
  AND tgt.charge_type IN ('ECS', 'EC3')
  AND m.from_entity = 'Global'
  AND m.to_entity = 'Global'
  AND m.created_by = 'SYSTEM';

-- ===== 045-global-rule-criteria-currency-rollback.sql =====
ALTER TABLE global_rule_criteria
    DROP COLUMN IF EXISTS currency;
