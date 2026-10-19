-- Add missing PRODUCT_TYPE category values

INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'G_PRODUCT_TYPE'
         JOIN (VALUES
                   ('ATL','ATL','ATL'),('BIO','BIO','BIO'),('BLK','BLK','BLK'),
                   ('BLT','BLT','BLT'),('BNK','BNK','BNK'),('BPL','BPL','BPL'),
                   ('BPR','BPR','BPR'),('CAC','CAC','CAC'),('CDA','CDA','CDA'),
                   ('CDS','CDS','CDS'),('CGO','CGO','CGO'),('COM','COM','COM'),
                   ('DEC','DEC','DEC'),('DEX','DEX','DEX'),('EMD','EMD','EMD'),
                   ('FIX','FIX','FIX'),('FTL','FTL','FTL'),('HVY','HVY','HVY'),
                   ('LEX','LEX','LEX'),('LGS','LGS','LGS'),('OND','OND','OND'),
                   ('ONP','ONP','ONP'),('OVL','OVL','OVL'),('P1H','P1H','P1H'),
                   ('P24','P24','P24'),('P2H','P2H','P2H'),('P30','P30','P30'),
                   ('P48','P48','P48'),('PEC','PEC','PEC'),('PEX','PEX','PEX'),
                   ('RTS','RTS','RTS'),('SDD','SDD','SDD'),('SFC','SFC','SFC'),
                   ('SMD','SMD','SMD'),('SMP','SMP','SMP'),('SMT','SMT','SMT'),
                   ('SPD','SPD','SPD'),('SPL','SPL','SPL'),('TCM','TCM','TCM'),
                   ('TCR','TCR','TCR'),('WPS','WPS','WPS'),('ABX','ABX','ABX'),
                   ('AMX','AMX','AMX'),('BBX','BBX','BBX'),('CGX','CGX','CGX'),
                   ('CPX','CPX','CPX'),('DGG','DGG','DGG'),('DGX','DGX','DGX'),
                   ('DPX','DPX','DPX'),('EPP','EPP','EPP'),('EPX','EPX','EPX'),
                   ('ETP','ETP','ETP'),('EUP','EUP','EUP'),('FCP','FCP','FCP'),
                   ('FDA','FDA','FDA'),('FDX','FDX','FDX'),('GDX','GDX','GDX'),
                   ('GPX','GPX','GPX'),('HGX','HGX','HGX'),('HPX','HPX','HPX'),
                   ('HWX','HWX','HWX'),('IBD','IBD','IBD'),('ICE','ICE','ICE'),
                   ('MPX','MPX','MPX'),('MYD','MYD','MYD'),('MYP','MYP','MYP'),
                   ('PME','PME','PME'),('PMI','PMI','PMI'),('PPX','PPX','PPX'),
                   ('PXD','PXD','PXD'),('PXP','PXP','PXP'),('SDG','SDG','SDG'),
                   ('SPX','SPX','SPX'),('VPX','VPX','VPX'),('A/F','A/F','A/F'),
                   ('CnD','CnD','CnD'),('L/F','L/F','L/F'),('S/F','S/F','S/F'),
                   ('EXP','EXP','EXP'),('DHD','DHD','DHD'),('DHP','DHP','DHP'),
                   ('SUN','SUN','SUN'),('XPX','XPX','XPX')
) v(code, name, label) ON TRUE
WHERE c.code = 'PRODUCT_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);
