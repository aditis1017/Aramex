-- Rollback SA global_setting + global_rule_criteria seed (033)

DELETE FROM global_rule_criteria
WHERE id IN (
    '454612c1-2794-473a-9439-572d7980cf08'::uuid,
    '75a80188-9db6-42e2-bbf8-6b75e60b2ebd'::uuid,
    '2be4c036-0a34-4ed0-bc33-6ec974a510df'::uuid,
    'bbd6afea-b7f4-45f5-a3ce-62ffaf0b6b4d'::uuid,
    'd371c432-cc08-4eef-b343-b66f96ac819f'::uuid,
    '869ab9a4-3b1a-4d2e-aad9-391598a251c0'::uuid,
    'b42c68bd-6948-4cdb-be65-a070a1d114c8'::uuid,
    'aa479538-0ea2-4ea4-be9a-6506ec5636a0'::uuid,
    '8b7a7271-1f64-4406-94de-8351b9fe662b'::uuid,
    '818b01ac-f673-43cf-856b-dbbe4f644f74'::uuid,
    '7e243f0f-6104-441d-92d6-7687d5cc4cb3'::uuid,
    '950936ea-1ada-48ac-91dc-1e44011ac715'::uuid,
    '2f33049d-7ae5-47d3-b2a5-f16889f74758'::uuid,
    'df7c703e-528c-4384-96cc-efad1e196934'::uuid,
    'ee14342e-63a0-44ad-9459-6c00c40136ba'::uuid,
    '4f20ff29-1715-4d59-b582-4334185533f8'::uuid,
    'f8473e02-4871-469d-98d1-fd991b032ec8'::uuid,
    'f21b1dad-0514-46c7-a03c-a4d85389cedc'::uuid,
    '2a5e7175-a46a-42b8-b9c5-f54ba4d463e3'::uuid,
    '1c2eb2ac-4c10-4fad-a7da-4a80774a3916'::uuid,
    'a4d76f4e-178f-4651-8af8-303d98cde8aa'::uuid,
    '33f1bfc8-e8cd-4ea1-8f00-3293c2d983bb'::uuid,
    'c0f6f0de-01ab-449d-87ea-adf2ec529887'::uuid,
    'aa5728a0-e4b2-45e1-b08b-a82911bdac94'::uuid,
    '852d1a52-f907-43f4-8ba0-31b20c07c602'::uuid,
    'ea3286ce-358e-43f0-8167-cf297fa8a31c'::uuid,
    '97a11ffb-be5e-47e2-86a4-a77f80fd797e'::uuid,
    'da210295-3a71-45c4-822a-4f47134225d5'::uuid,
    '7340b45e-0080-444c-911e-faf2a8cd3482'::uuid,
    '3ef6b22a-d7f6-4cc8-a15c-4d15cac93c62'::uuid,
    'e1586727-377e-4656-a649-e18c4bb31180'::uuid,
    'cf64a188-a853-424a-9432-f85820cd8a0a'::uuid
);

DELETE FROM global_setting
WHERE id IN (
    '28436ef9-c97b-4522-98ee-d00eadbe4188'::uuid,
    'dd1eee6a-1613-486f-a717-04b4104e47d1'::uuid,
    '89633da3-ae3a-458f-84d0-46effa1e4e05'::uuid,
    '858ff7e1-f829-49bc-bf94-24862ae43b18'::uuid,
    'd9277fde-79b7-4176-9ada-4e1cb7b6e3de'::uuid,
    'd40105f3-93d3-45a7-b0f4-7895ec8a48b9'::uuid,
    'a0a8968a-9247-48bf-8e96-2ec684f6c9d0'::uuid,
    'c67f89ef-48cd-4210-bae2-23409d9dbe0a'::uuid,
    '50bae154-9a23-4c69-84c0-804ab70f588f'::uuid,
    'fd5c1032-d348-4868-b22c-af91ca970438'::uuid,
    '7d746de9-6689-44eb-ac8c-dee6d713255c'::uuid,
    'd172f6a0-ceb5-44c0-9176-1c13ecdef078'::uuid,
    'efa1e33b-0272-4163-b4f3-d543b90338e7'::uuid,
    'dff06cbf-6080-4d8e-8d80-4839dc77867f'::uuid,
    '09807c2e-7b58-4781-bccc-7682157dd062'::uuid,
    '7ebf01ee-40cf-49a8-b750-ba12f18f9267'::uuid,
    '8b64cf66-dbd4-4a3f-8f5d-8dc49be74722'::uuid,
    '8e2f2942-cf88-4d1b-9dd6-c780d8d22a78'::uuid
);
