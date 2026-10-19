-- Entity-first dashboard pagination (entity_id predicate + ORDER BY created_time)
CREATE INDEX IF NOT EXISTS idx_charge_entity_created_time
    ON charge (entity_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_charge_entity_code_created_time
    ON charge (entity_id, charge_code, created_time DESC);
