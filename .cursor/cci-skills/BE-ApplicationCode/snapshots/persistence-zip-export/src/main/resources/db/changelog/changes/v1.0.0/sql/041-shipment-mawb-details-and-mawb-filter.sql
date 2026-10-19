CREATE TABLE IF NOT EXISTS shipment_mawb_details (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    hawb_number VARCHAR(100) NOT NULL,
    mawb_number VARCHAR(100) NOT NULL,
    source_entity VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100) NOT NULL,
    CONSTRAINT uk_shipment_mawb_details_hawb_mawb_source_target
        UNIQUE (hawb_number, mawb_number, source_entity, target_entity)
);

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('MAWB_FILTER', 'MAWB_FILTER', 'MAWB Filter Rule')
) v(code, name, label)
WHERE c.code = 'SYSTEM_RULES'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = 'MAWB_FILTER'
);

INSERT INTO global_setting (
    sequence_number,
    status,
    created_time,
    updated_time,
    id,
    target_field,
    target_value,
    created_by,
    destination_code,
    drl_rule,
    rule_condition,
    rule_signature,
    updated_by
)
SELECT
    1,
    true,
    TIMESTAMPTZ '2026-05-19 09:55:08.473+00',
    TIMESTAMPTZ '2026-05-19 09:55:08.473+00',
    'a3c8f1e2-4b5d-6e7f-8a9b-0c1d2e3f4a5b'::uuid,
    'Customs Type',
    'MAWB_FILTER',
    'SYSTEM',
    'SA',
    $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        piNumber == "SH022" &&
        destinationEntity in ("RUH", "JED", "DHA")
    )
then
    $shipment.setIsMawbShipment(true);
    update($shipment);
end
$drl$,
    'SH022 && DestinationEntity in (RUH, JED, DHA)',
    NULL,
    'SYSTEM'
WHERE NOT EXISTS (
    SELECT 1
    FROM global_setting gs
    WHERE gs.destination_code = 'SA'
      AND gs.target_value = 'MAWB_FILTER'
);
