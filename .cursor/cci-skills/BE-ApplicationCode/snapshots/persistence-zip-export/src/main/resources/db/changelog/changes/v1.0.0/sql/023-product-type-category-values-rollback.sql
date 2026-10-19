-- Rollback PRODUCT_TYPE category values added in 023

DELETE FROM category_value
WHERE code IN (
    'ATL', 'BIO', 'BLK', 'BLT', 'BNK', 'BPL', 'BPR', 'CAC', 'CDA', 'CDS',
    'CGO', 'COM', 'DEC', 'DEX', 'EMD', 'FIX', 'FTL', 'HVY', 'LEX', 'LGS',
    'OND', 'ONP', 'OVL', 'P1H', 'P24', 'P2H', 'P30', 'P48', 'PEC', 'PEX',
    'RTS', 'SDD', 'SFC', 'SMD', 'SMP', 'SMT', 'SPD', 'SPL', 'TCM', 'TCR',
    'WPS', 'ABX', 'AMX', 'BBX', 'CGX', 'CPX', 'DGG', 'DGX', 'DPX', 'EPP',
    'EPX', 'ETP', 'EUP', 'FCP', 'FDA', 'FDX', 'GDX', 'GPX', 'HGX', 'HPX',
    'HWX', 'IBD', 'ICE', 'MPX', 'MYD', 'MYP', 'PME', 'PMI', 'PPX', 'PXD',
    'PXP', 'SDG', 'SPX', 'VPX', 'A/F', 'CnD', 'L/F', 'S/F', 'EXP', 'DHD',
    'DHP', 'SUN', 'XPX'
);
