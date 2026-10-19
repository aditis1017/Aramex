-- Seed SA global_setting + global_rule_criteria from production snapshot.
-- Runs only when global_setting has <= 3 rows (see changelog precondition).
-- Skips SHIPMENT_FILTER if already seeded by 006-reference-data-seed.

-- ============================================================
-- global_setting
-- ============================================================

INSERT INTO global_setting (
    id, created_by, updated_by, created_time, updated_time,
    destination_code, target_field, target_value, status, sequence_number,
    rule_signature, drl_rule, rule_condition
)
SELECT
    v.id, v.created_by, v.updated_by, v.created_time::timestamptz, v.updated_time::timestamptz,
    v.destination_code, v.target_field, v.target_value, v.status, v.sequence_number,
    v.rule_signature, v.drl_rule, v.rule_condition
FROM (
    VALUES
        (
            '28436ef9-c97b-4522-98ee-d00eadbe4188'::uuid,
            'mayank.mishra@tothenew.com',
            'ZaidAll@aramex.com',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-13 10:34:10.958434+00',
            'SA',
            'CustomType',
            'DDP',
            TRUE,
            1,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        productType in ("DIM","EPX","LHS")
    )
then
    $shipment.setShipmentType("DDP");
    update($shipment);
end


$drl$,
            'Product Type in (DIM,EPX,LHS)'
        ),
        (
            'dd1eee6a-1613-486f-a717-04b4104e47d1'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.866168+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            1,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        productGroup == "DOM"
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Product Group = DOM'
        ),
        (
            '89633da3-ae3a-458f-84d0-46effa1e4e05'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.866089+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            2,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("C","F")
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Payment Type in (C,F)'
        ),
        (
            '858ff7e1-f829-49bc-bf94-24862ae43b18'::uuid,
            'mayank.mishra@tothenew.com',
            'ekansh.tyagi@tothenew.com',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-13 07:06:59.008348+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            3,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        thirdPartyEntity in ("DHA","JED","RUH") && paymentType == "3"
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Payment Type = 3 AND Third Party Entity in (DHA,JED,RUH)'
        ),
        (
            'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid,
            'mayank.mishra@tothenew.com',
            'ZaidAll@aramex.com',
            '2026-09-11 04:22:28.71982+00',
            '2026-09-13 14:23:51.303999+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            6,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        businessType == "B2B" && paymentType in ("3","P") && shipmentType != "DDP" && frdmFlag == "No" && productType not in ("DIM","EPX","LHS")
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Business Type = B2B AND Customs Type != DDP AND FRDM Flag = No AND Payment Type in (3,P) AND Product Type not in (DIM,EPX,LHS)'
        ),
        (
            'd40105f3-93d3-45a7-b0f4-7895ec8a48b9'::uuid,
            'alok.agrawal@tothenew.com',
            'alok.agrawal@tothenew.com',
            '2026-09-13 06:48:59.200669+00',
            '2026-09-13 06:48:59.200669+00',
            'SA',
            'Shipment Category',
            'Exempted',
            TRUE,
            3,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        shipmentType == "DDO"
    )
then
    $shipment.setCategoryType("Exempted");
    update($shipment);
end


$drl$,
            'Customs Type = DDO'
        ),
        (
            'a0a8968a-9247-48bf-8e96-2ec684f6c9d0'::uuid,
            'ekansh.tyagi@tothenew.com',
            'ekansh.tyagi@tothenew.com',
            '2026-09-13 07:42:10.454492+00',
            '2026-09-13 07:42:10.454492+00',
            'SA',
            'Shipment Category',
            'Exempted',
            TRUE,
            4,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        productType in ("RDG","RDL","RMY","RTC","RTD","RTI","RTN","RTP")
    )
then
    $shipment.setCategoryType("Exempted");
    update($shipment);
end


$drl$,
            'Product Type in (RDG,RDL,RMY,RTC,RTD,RTI,RTN,RTP)'
        ),
        (
            'c67f89ef-48cd-4210-bae2-23409d9dbe0a'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.86584+00',
            'SA',
            'CustomType',
            'DDP',
            TRUE,
            2,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("3","P") && shipmentType == "DDP"
    )
then
    $shipment.setShipmentType("DDP");
    update($shipment);
end


$drl$,
            'Customs Type = DDP AND Payment Type in (3,P)'
        ),
        (
            '50bae154-9a23-4c69-84c0-804ab70f588f'::uuid,
            'mayank.mishra@tothenew.com',
            'ZaidAll@aramex.com',
            '2026-09-10 11:16:52.650095+00',
            '2026-09-13 08:15:55.345507+00',
            'SA',
            'CustomType',
            'DDP',
            TRUE,
            3,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("3","P") && frdmFlag == "Yes"
    )
then
    $shipment.setShipmentType("DDP");
    update($shipment);
end


$drl$,
            'FRDM Flag = Yes AND Payment Type in (3,P)'
        ),
        (
            'fd5c1032-d348-4868-b22c-af91ca970438'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-09-11 14:11:59.699142+00',
            '2026-09-14 00:00:02.698623+00',
            'SA',
            'CustomType',
            'DDP',
            TRUE,
            4,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        businessType == "B2C" && paymentType in ("3","P")
    )
then
    $shipment.setShipmentType("DDP");
    update($shipment);
end


$drl$,
            'Business Type = B2C AND Payment Type in (3,P)'
        ),
        (
            '7d746de9-6689-44eb-ac8c-dee6d713255c'::uuid,
            'mayank.mishra@tothenew.com',
            'ZaidAll@aramex.com',
            '2026-09-10 11:16:07.331518+00',
            '2026-09-13 10:52:43.868871+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            5,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("3","P") && shipmentType == "DDU" && frdmFlag == "No"
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Customs Type = DDU AND FRDM Flag = No AND Payment Type in (3,P)'
        ),
        (
            'd172f6a0-ceb5-44c0-9176-1c13ecdef078'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.866539+00',
            'SA',
            'Shipment Category',
            'Exempted',
            TRUE,
            1,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        shipperCountryCode == "SA" && consigneeCountryCode == "SA" && productGroup == "EXP"
    )
then
    $shipment.setCategoryType("Exempted");
    update($shipment);
end


$drl$,
            'Destination = Saudi Arabia AND Origin = Saudi Arabia AND Product Group = EXP'
        ),
        (
            'efa1e33b-0272-4163-b4f3-d543b90338e7'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.86593+00',
            'SA',
            'Shipment Category',
            'HV',
            TRUE,
            5,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        costOfGoods >= 1000
    )
then
    $shipment.setCategoryType("HV");
    update($shipment);
end


$drl$,
            'CIF >= 1000'
        ),
        (
            'dff06cbf-6080-4d8e-8d80-4839dc77867f'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-09-11 10:30:52.900499+00',
            '2026-09-11 15:43:40.866013+00',
            'SA',
            'CustomType',
            'DDO',
            TRUE,
            7,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("3","P") && shipmentType == "DDO"
    )
then
    $shipment.setShipmentType("DDO");
    update($shipment);
end


$drl$,
            'Customs Type = DDO AND Payment Type in (3,P)'
        ),
        (
            '09807c2e-7b58-4781-bccc-7682157dd062'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.86645+00',
            'SA',
            'Shipment Category',
            'LV',
            TRUE,
            6,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        costOfGoods < 1000
    )
then
    $shipment.setCategoryType("LV");
    update($shipment);
end


$drl$,
            'CIF < 1000'
        ),
        (
            '7ebf01ee-40cf-49a8-b750-ba12f18f9267'::uuid,
            'mayank.mishra@tothenew.com',
            'SYSTEM',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-11 15:43:40.865535+00',
            'SA',
            'CustomType',
            'DDU',
            TRUE,
            4,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        paymentType in ("3","P") && productType in ("RDG","RDL","RMY","RTC","RTD","RTI","RTN","RTP")
    )
then
    $shipment.setShipmentType("DDU");
    update($shipment);
end


$drl$,
            'Payment Type in (3,P) AND Product Type in (RDG,RDL,RMY,RTC,RTD,RTI,RTN,RTP)'
        ),
        (
            '8b64cf66-dbd4-4a3f-8f5d-8dc49be74722'::uuid,
            'mayank.mishra@tothenew.com',
            'ZaidAll@aramex.com',
            '2026-08-23 07:39:58.373898+00',
            '2026-09-15 06:45:22.50189+00',
            'SA',
            'Shipment Category',
            'Exempted',
            TRUE,
            2,
            NULL::varchar,
            $drl$package rules

rule "GlobalRule"
no-loop true
when
    $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
        costOfGoods == "0" && productType in ("CDX","DDX","EDX","GDX","MDX","PDX","PLX","PXD","SDX")
    )
then
    $shipment.setCategoryType("Exempted");
    update($shipment);
end


$drl$,
            'Cost of Goods = 0 AND Product Type in (CDX,DDX,EDX,GDX,MDX,PDX,PLX,PXD,SDX)'
        )
) AS v(
    id, created_by, updated_by, created_time, updated_time,
    destination_code, target_field, target_value, status, sequence_number,
    rule_signature, drl_rule, rule_condition
)
WHERE NOT EXISTS (
    SELECT 1 FROM global_setting gs WHERE gs.id = v.id
);
GO

-- SHIPMENT_FILTER (only if not already seeded by 006)
INSERT INTO global_setting (
    id, created_by, updated_by, created_time, updated_time,
    destination_code, target_field, target_value, status, sequence_number,
    rule_signature, drl_rule, rule_condition
)
SELECT
    '8e2f2942-cf88-4d1b-9dd6-c780d8d22a78'::uuid,
    'SYSTEM',
    'SYSTEM',
    '2026-08-23 07:39:58.373898+00'::timestamptz,
    '2026-08-23 07:39:58.373898+00'::timestamptz,
    'SA',
    'Customs Type',
    'SHIPMENT_FILTER',
    TRUE,
    1,
    NULL,
    $drl$package rules
          rule "GlobalRule"
          when
             $shipment : com.customcharge.persistencelayer.dto.ShipmentDto(
                 piNumber == "SH013" &&
                 destinationEntity in ("RUH", "JED", "DHA") &&
                 (
                     productGroup != null &&
                     (
                         productGroup.toUpperCase() == "EXP" ||
                         (
                             productGroup.toUpperCase() == "DOM" &&
                             shipperAccountNumber == "72104493"
                         )
                     )
                 )
             )
          then
             $shipment.setIsShipmentFull(true);
             update($shipment);
             System.out.println("Rule Executed : GlobalRule");
          end
          $drl$,
    'SH013 && DestinationEntity in (RUH, JED, DHA) && (EXP || (DOM && 72104493))'
WHERE NOT EXISTS (
    SELECT 1 FROM global_setting gs WHERE gs.id = '8e2f2942-cf88-4d1b-9dd6-c780d8d22a78'::uuid
)
  AND NOT EXISTS (
    SELECT 1
    FROM global_setting gs
    WHERE gs.destination_code = 'SA'
      AND gs.target_value = 'SHIPMENT_FILTER'
);
GO

-- ============================================================
-- global_rule_criteria
-- ============================================================

INSERT INTO global_rule_criteria (
    id, created_by, updated_by, created_time, updated_time,
    global_setting_id, field_name, operator, "values", exact_value, currency
)
SELECT
    v.id, v.created_by, v.updated_by, v.created_time::timestamptz, v.updated_time::timestamptz,
    v.global_setting_id, v.field_name, v.operator, v.values_text, v.exact_value, v.currency
FROM (
    VALUES
        ('454612c1-2794-473a-9439-572d7980cf08'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-10 11:16:07.371893+00', '2026-09-10 11:16:07.371893+00', '7d746de9-6689-44eb-ac8c-dee6d713255c'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('75a80188-9db6-42e2-bbf8-6b75e60b2ebd'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-10 11:16:52.658383+00', '2026-09-10 11:16:52.658383+00', '50bae154-9a23-4c69-84c0-804ab70f588f'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('2be4c036-0a34-4ed0-bc33-6ec974a510df'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:52:10.665892+00', '2026-09-11 10:32:17.214404+00', '858ff7e1-f829-49bc-bf94-24862ae43b18'::uuid, 'G_THIRD_PARTY_ENTITY', 'IN', 'RUH,JED,DHA', NULL, 'SAR'),
        ('bbd6afea-b7f4-45f5-a3ce-62ffaf0b6b4d'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:53:22.147172+00', '2026-09-11 10:32:33.774116+00', '7ebf01ee-40cf-49a8-b750-ba12f18f9267'::uuid, 'G_PAYMENT_TYPE', 'IN', 'P,3', NULL, 'SAR'),
        ('d371c432-cc08-4eef-b343-b66f96ac819f'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:53:22.14729+00', '2026-09-11 10:32:33.774431+00', '7ebf01ee-40cf-49a8-b750-ba12f18f9267'::uuid, 'G_PRODUCT_TYPE', 'IN', 'RDL,RTC,RMY,RTD,RTI,RTP,RDG,RTN', NULL, 'SAR'),
        ('869ab9a4-3b1a-4d2e-aad9-391598a251c0'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-09-11 10:30:30.748767+00', '2026-09-13 08:14:44.111257+00', 'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid, 'G_BUSINESS_TYPE', 'EQUALS', 'B2B_BT', NULL, 'SAR'),
        ('b42c68bd-6948-4cdb-be65-a070a1d114c8'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 04:22:28.720106+00', '2026-09-11 04:22:28.720106+00', 'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('aa479538-0ea2-4ea4-be9a-6506ec5636a0'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-10 11:17:26.239937+00', '2026-09-11 04:23:21.399787+00', 'c67f89ef-48cd-4210-bae2-23409d9dbe0a'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('8b7a7271-1f64-4406-94de-8351b9fe662b'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-09-11 10:35:06.83392+00', '2026-09-13 08:15:55.286001+00', '50bae154-9a23-4c69-84c0-804ab70f588f'::uuid, 'G_FRDM_FLAG', 'EQUALS', 'FRDM_YES', NULL, 'SAR'),
        ('818b01ac-f673-43cf-856b-dbbe4f644f74'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:55:26.432456+00', '2026-09-11 10:35:53.207887+00', 'efa1e33b-0272-4163-b4f3-d543b90338e7'::uuid, 'G_CIF', 'GREATERTHANEQUALS', NULL, '1000', 'SAR'),
        ('7e243f0f-6104-441d-92d6-7687d5cc4cb3'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 10:30:30.722984+00', '2026-09-11 10:30:30.722984+00', 'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid, 'G_CUSTOMS_TYPE', 'NOT_EQUALS', 'DDP', NULL, 'SAR'),
        ('950936ea-1ada-48ac-91dc-1e44011ac715'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 04:22:28.720222+00', '2026-09-11 10:30:30.771437+00', 'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid, 'G_FRDM_FLAG', 'EQUALS', 'FRDM_NO', NULL, 'SAR'),
        ('2f33049d-7ae5-47d3-b2a5-f16889f74758'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 10:30:52.901099+00', '2026-09-11 10:30:52.901099+00', 'dff06cbf-6080-4d8e-8d80-4839dc77867f'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('df7c703e-528c-4384-96cc-efad1e196934'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 10:31:08.026722+00', '2026-09-11 10:31:08.026722+00', 'dff06cbf-6080-4d8e-8d80-4839dc77867f'::uuid, 'G_CUSTOMS_TYPE', 'EQUALS', 'DDO', NULL, 'SAR'),
        ('ee14342e-63a0-44ad-9459-6c00c40136ba'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:51:21.342688+00', '2026-09-11 10:31:48.794948+00', '89633da3-ae3a-458f-84d0-46effa1e4e05'::uuid, 'G_PAYMENT_TYPE', 'IN', 'C,F', NULL, 'SAR'),
        ('4f20ff29-1715-4d59-b582-4334185533f8'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-28 13:11:52.870478+00', '2026-09-11 10:32:05.342548+00', 'dd1eee6a-1613-486f-a717-04b4104e47d1'::uuid, 'G_PRODUCT_GROUP', 'EQUALS', 'DOM', NULL, 'SAR'),
        ('f8473e02-4871-469d-98d1-fd991b032ec8'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:52:10.665766+00', '2026-09-11 10:32:17.214005+00', '858ff7e1-f829-49bc-bf94-24862ae43b18'::uuid, 'G_PAYMENT_TYPE', 'EQUALS', '3', NULL, 'SAR'),
        ('f21b1dad-0514-46c7-a03c-a4d85389cedc'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-10 11:16:07.372528+00', '2026-09-11 10:32:48.274509+00', '7d746de9-6689-44eb-ac8c-dee6d713255c'::uuid, 'G_CUSTOMS_TYPE', 'EQUALS', 'DDU', NULL, 'SAR'),
        ('2a5e7175-a46a-42b8-b9c5-f54ba4d463e3'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-09-11 14:11:59.699738+00', '2026-09-13 08:16:09.310292+00', 'fd5c1032-d348-4868-b22c-af91ca970438'::uuid, 'G_BUSINESS_TYPE', 'EQUALS', 'B2C_BT', NULL, 'SAR'),
        ('1c2eb2ac-4c10-4fad-a7da-4a80774a3916'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:55:59.968528+00', '2026-09-11 10:36:29.743159+00', '09807c2e-7b58-4781-bccc-7682157dd062'::uuid, 'G_CIF', 'LESSTHAN', NULL, '1000', 'SAR'),
        ('a4d76f4e-178f-4651-8af8-303d98cde8aa'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-23 10:13:27.739511+00', '2026-09-11 10:34:47.592961+00', 'c67f89ef-48cd-4210-bae2-23409d9dbe0a'::uuid, 'G_CUSTOMS_TYPE', 'EQUALS', 'DDP', NULL, 'SAR'),
        ('33f1bfc8-e8cd-4ea1-8f00-3293c2d983bb'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:54:18.86277+00', '2026-09-11 10:35:16.934514+00', 'd172f6a0-ceb5-44c0-9176-1c13ecdef078'::uuid, 'G_ORIGIN', 'EQUALS', 'SA', NULL, 'SAR'),
        ('c0f6f0de-01ab-449d-87ea-adf2ec529887'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:54:18.86294+00', '2026-09-11 10:35:16.934748+00', 'd172f6a0-ceb5-44c0-9176-1c13ecdef078'::uuid, 'G_DESTINATION', 'EQUALS', 'SA', NULL, 'SAR'),
        ('aa5728a0-e4b2-45e1-b08b-a82911bdac94'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-14 09:54:18.863049+00', '2026-09-11 10:35:16.934821+00', 'd172f6a0-ceb5-44c0-9176-1c13ecdef078'::uuid, 'G_PRODUCT_GROUP', 'EQUALS', 'EXP', NULL, 'SAR'),
        ('852d1a52-f907-43f4-8ba0-31b20c07c602'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-07-14 09:53:42.64324+00', '2026-09-13 10:34:10.922845+00', '28436ef9-c97b-4522-98ee-d00eadbe4188'::uuid, 'G_PRODUCT_TYPE', 'IN', 'DIM,LHS,EPX', NULL, 'SAR'),
        ('ea3286ce-358e-43f0-8167-cf297fa8a31c'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-07-23 10:59:08.235485+00', '2026-09-11 10:35:33.3344+00', '8b64cf66-dbd4-4a3f-8f5d-8dc49be74722'::uuid, 'G_COST_OF_GOODS', 'EQUALS', NULL, '0', 'SAR'),
        ('97a11ffb-be5e-47e2-86a4-a77f80fd797e'::uuid, 'mayank.mishra@tothenew.com', 'mayank.mishra@tothenew.com', '2026-09-11 14:11:59.699589+00', '2026-09-11 14:11:59.699589+00', 'fd5c1032-d348-4868-b22c-af91ca970438'::uuid, 'G_PAYMENT_TYPE', 'IN', '3,P', NULL, 'SAR'),
        ('da210295-3a71-45c4-822a-4f47134225d5'::uuid, 'alok.agrawal@tothenew.com', 'alok.agrawal@tothenew.com', '2026-09-13 06:48:59.201736+00', '2026-09-13 06:48:59.201736+00', 'd40105f3-93d3-45a7-b0f4-7895ec8a48b9'::uuid, 'G_CUSTOMS_TYPE', 'EQUALS', 'DDO', NULL, 'SAR'),
        ('7340b45e-0080-444c-911e-faf2a8cd3482'::uuid, 'ekansh.tyagi@tothenew.com', 'ekansh.tyagi@tothenew.com', '2026-09-13 07:42:10.463623+00', '2026-09-13 07:42:10.463623+00', 'a0a8968a-9247-48bf-8e96-2ec684f6c9d0'::uuid, 'G_PRODUCT_TYPE', 'IN', 'RDL,RTC,RMY,RTN,RTD,RTI,RDG,RTP', NULL, 'SAR'),
        ('3ef6b22a-d7f6-4cc8-a15c-4d15cac93c62'::uuid, 'ZaidAll@aramex.com', 'ZaidAll@aramex.com', '2026-09-13 10:52:43.825208+00', '2026-09-13 10:52:43.825208+00', '7d746de9-6689-44eb-ac8c-dee6d713255c'::uuid, 'G_FRDM_FLAG', 'EQUALS', 'FRDM_NO', NULL, 'SAR'),
        ('e1586727-377e-4656-a649-e18c4bb31180'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-09-12 13:19:42.092559+00', '2026-09-13 14:23:51.259467+00', 'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid, 'G_PRODUCT_TYPE', 'NOT_IN', 'DIM,LHS,EPX', NULL, 'SAR'),
        ('cf64a188-a853-424a-9432-f85820cd8a0a'::uuid, 'mayank.mishra@tothenew.com', 'ZaidAll@aramex.com', '2026-07-14 09:55:00.46028+00', '2026-09-15 06:45:21.717062+00', '8b64cf66-dbd4-4a3f-8f5d-8dc49be74722'::uuid, 'G_PRODUCT_TYPE', 'IN', 'EDX,SDX,DDX,MDX,PDX,PLX,CDX,PXD,GDX', NULL, 'SAR')
) AS v(
    id, created_by, updated_by, created_time, updated_time,
    global_setting_id, field_name, operator, values_text, exact_value, currency
)
WHERE EXISTS (
    SELECT 1 FROM global_setting gs WHERE gs.id = v.global_setting_id
)
  AND NOT EXISTS (
    SELECT 1 FROM global_rule_criteria grc WHERE grc.id = v.id
);
GO
