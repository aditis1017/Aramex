-- Reference data seed (currency, category values, charge type doc id mapping)
INSERT INTO currency
(currency_code, currency_name, country_code, is_active)
VALUES
    ('JPY','Yen','JP',true),
    ('BGN','Lev','BG',true),
    ('EUR','Euro','FR',true),
    ('UAH','Hryvnia','UA',true),
    ('KRW','Won','KR',true),
    ('KZT','Tenge','KZ',true),
    ('EUR','Euro','DE',true),
    ('RON','Leu','RO',true),
    ('NOK','Norwegian Krone','NO',true),
    ('EUR','Euro','CY',true),
    ('EUR','Euro','ES',true),
    ('EUR','Euro','IT',true),
    ('EUR','Euro','GR',true),
    ('EUR','Euro','LT',true),
    ('HUF','Forint','HU',true),
    ('EUR','Euro','BE',true),
    ('SEK','Swedish Krona','SE',true),
    ('TWD','New Taiwan Dollar','TW',true),
    ('PHP','Philippine Peso','PH',true),
    ('UZS','Uzbekistani Som','UZ',true),
    ('NPR','Nepalese Rupee','NP',true),
    ('ETB','Birr','ET',true),
    ('AZN','Manat','AZ',true),
    ('DKK','Danish Krone','DK',true),
    ('EUR','Euro','FI',true),
    ('CHF','Swiss Franc','CH',true),
    ('VND','Viet Nam Dong','VN',true),
    ('EUR','Euro','AT',true),
    ('PKR','Pakistani Rupee','PK',true),
    ('BRL','Brazilian Real','BR',true),
    ('MOP','Pataca','MO',true),
    ('SYP','Syrian Pound','SY',true),
    ('HKD','Hong Kong Dollar','HK',true),
    ('KWD','Kuwaiti Dinar','KW',true),
    ('QAR','Qatari Riyal','QA',true),
    ('GBP','British Pound','GB',true),
    ('MAD','Moroccan Dirham','MA',true),
    ('TRY','Turkish Lira','TR',true),
    ('AUD','Australian Dollar','AU',true),
    ('BHD','Bahraini Dinar','BH',true),
    ('AMD','Dram','AM',true),
    ('EGP','Egyptian Pound','EG',true),
    ('MYR','Ringgit','MY',true),
    ('CAD','Canadian Dollar','CA',true),
    ('OMR','Omani Rial','OM',true),
    ('EUR','Euro','NL',true),
    ('CZK','Koruna','CZ',true),
    ('TZS','Tanzanian Shilling','TZ',true),
    ('JOD','Jordanian Dinar','JO',true),
    ('AED','Dirham','AE',true),
    ('NZD','New Zealand Dollar','NZ',true),
    ('CNY','Yuan','CN',true),
    ('TND','Tunisian Dinar','TN',true),
    ('EUR','Euro','IE',true),
    ('BDT','Taka','BD',true),
    ('GHS','Ghana Cedi','GH',true),
    ('THB','Baht','TH',true),
    ('SGD','Singapore Dollar','SG',true),
    ('LBP','Lebanese Pound','LB',true),
    ('IDR','Indonesian Rupiah','ID',true),
    ('ZAR','Rand','ZA',true),
    ('KES','Kenyan Shilling','KE',true),
    ('ILS','New Israeli Shekel','PS',true),
    ('SDG','Sudanese Pound','SD',true),
    ('GEL','Lari','GE',true),
    ('LKR','Sri Lankan Rupee','LK',true),
    ('UGX','Ugandan Shilling','UG',true),
    ('NGN','Naira','NG',true),
    ('INR','Indian Rupee','IN',true),
    ('IQD','Iraqi Dinar','IQ',true),
    ('LYD','Libyan Dinar','LY',true),
    ('USD','United States Dollar','US',true),
    ('SAR','Saudi Riyal','SA',true),
    ('SOS','Somaliland Shilling','OS',true)
;

-- =========================
-- CATEGORY (MERGED)
-- =========================
INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('CHARGE_TYPE', 'Charge Type'),
          ('ACCOUNT_SEARCH_FIELD', 'Account Search Field'),
          ('ACCOUNT_SEARCH_TYPE', 'Account Search Type'),
          ('DEST_COST_STATUS', 'Dest Cost Status'),
          ('CURRENCY', 'Currency'),
          ('INVOICE_TRIGGER', 'Invoice Trigger'),
          ('MARKET_STRUCTURE', 'Market Structure'),
          ('CUSTOMS_TYPE', 'Customs Type'),
          ('INVOICE_FREQUENCY', 'Invoice Frequency'),
          ('BUSINESS_TYPE', 'Business Type'),
          ('CATEGORY_TYPE', 'Category type'),
          ('PRODUCT_GROUP', 'Product Group'),
          ('PAYMENT_TYPE', 'Payment TYPE'),
          ('GLOBAL_RULE', 'Globle Rule'),
          ('PRODUCT_TYPE', 'Product Type'),
          ('ELIGIBLE_FOR_BILLING', 'Eligible For Billing'),
          ('INVOICE_BREAK_BY', 'Invoice Break By'),
          ('INVOICE_SHIPMENT_LIMIT', 'Invoice Shipment Limit'),
          ('INVOICE_AMOUNT_LIMIT', 'Invoice Amount Limit'),
          ('POTENTIAL_DELAY', 'Potential Delay'),
          ('SPLIT_ENTRY', 'Split Entry'),
          ('AMOUNT_TYPE', 'Amount Type'),
          ('BUSINESS_TYPE_AR', 'Business Type AR'),
          ('ACCOUNT', 'Account Type'),
          ('SERVICE_CODE', 'Service Code'),
          ('SPLIT_INVOICE', 'Split Invoice'),
          ('SERVICE_DESCRIPTION', 'Service Description'),
          ('ACCOUNT_STATUS', 'Account Status'),
          ('AP_ACCOUNT_TYPE', 'AP Account Type'),
          ('AP_ACCOUNT_NUMBER', 'AP Account Number'),
          ('CHARGE_FILE_TYPE', 'Charge File Type'),
          ('AR_ACCOUNT_NUMBER', 'Ar Account Number')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.code = v.code
);



-- =========================
-- CATEGORY VALUES HELPERS
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('DDP', 'DDP'),
                         ('DDU', 'DDU'),
                         ('DDO', 'DDO')
) v(code, name)
WHERE c.code = 'CUSTOMS_TYPE';


-- =========================
-- CATEGORY VALUES INVOICE_TRIGGER
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('MANUAL', 'Manual'),
                         ('AUTO', 'Auto')
) v(code, name)
WHERE c.code = 'INVOICE_TRIGGER';


-- =========================
-- CATEGORY VALUES Ar Account Number
-- =========================

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('DKMBB', 'DKMBB')
) v(code, name)
WHERE c.code = 'AR_ACCOUNT_NUMBER';

-- =========================
-- BUSINESS_TYPE_AR
-- =========================

INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('E-Commerce_BT', 'E-Commerce', 'E-Commerce'),
        ('WHOLESALE_BT', 'Wholesale', 'Wholesale'),
        ('CORPORATE_BUISNESS_BT', 'Corporate Business', 'Corporate Business'),
        ('OIL_AND_GAS_BT', 'Oil and Gas', 'Oil and Gas'),
        ('Retail_BT', 'Retail (Cash)', 'Retail (Cash)')
) v(code, name, label)
WHERE c.code = 'BUSINESS_TYPE_AR'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);



-- =========================
-- BUSINESS_TYPE
-- =========================

INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('B2B_BT', 'B2B', 'B2B'),
        ('B2C_BT', 'B2C', 'B2C')
) v(code, name, label)
WHERE c.code = 'BUSINESS_TYPE'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);



-- =========================
-- SPLIT_INVOICE
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('PER_SHIPMENT', 'Per Shipment'),
                         ('CONSOLIDATED_SHIPMENT', 'Consolidated Shipment')
) v(code, name)
WHERE c.code = 'SPLIT_INVOICE';




-- =========================
-- INVOICE_FREQUENCY
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('WEEKLY', 'Weekly'),
                         ('MONTHLY', 'Monthly')
) v(code, name)
WHERE c.code = 'INVOICE_FREQUENCY';


-- =========================
-- MARKET_STRUCTURE
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('DIRECT', 'Direct'),
                         ('INDIRECT', 'Indirect')

) v(code, name)
WHERE c.code = 'MARKET_STRUCTURE';



-- =========================
-- CURRENCY
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value
(category_id, code, name, label, is_active)

SELECT
    c.id,
    v.code,
    v.name,
    v.name,
    true
FROM category c
         CROSS JOIN (
    VALUES
        ('SAR', 'SAR'),
        ('USD', 'USD'),
        ('JOD', 'JOD')
) v(code, name)
WHERE c.code = 'CURRENCY';


-- =========================
-- CHARGE_FILE_TYPE
-- =========================

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('Actual_Template', 'Actuals-Upload-Template.xlsx','Actuals'),
                         ('Estimates_Template', 'Estimates-Upload-Template.xlsx','Estimates')
) v(code, name, label)
WHERE c.code = 'CHARGE_FILE_TYPE';



-- =========================
-- AP_ACCOUNT_TYPE
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('VENDOR', 'Vendor')
) v(code, name)
WHERE c.code = 'AP_ACCOUNT_TYPE';

-- =========================
-- AP_ACCOUNT_NUMBER
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('12345', '12345'),
                         ('DKMB', 'DKMB')
) v(code, name)
WHERE c.code = 'AP_ACCOUNT_NUMBER';

-- =========================
-- Actual and Estimate Template Charge
-- =========================

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('Template_Charges', 'Template Charges')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.code = v.code
);

INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'Estimates_Template'
         JOIN (VALUES
                   ('Estimates_Template_Charge','ECD','ECD')
) v(code, name, label) ON TRUE
WHERE c.code = 'Template_Charges'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'Actual_Template'
         JOIN (VALUES
                   ('Actual_Template_Charge','DTC','DTC')
) v(code, name, label) ON TRUE
WHERE c.code = 'Template_Charges'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);




-- =========================
-- INVOICE_BREAK_BY
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('HWB', 'HWB'),
                         ('CONSOLIDATED', 'CONSOLIDATED')
) v(code, name)
WHERE c.code = 'INVOICE_BREAK_BY';



-- =========================
-- INVOICE_SHIPMENT_LIMIT
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('5000', '5000'),
                         ('7000', '7000'),
                         ('10000', '10000')
) v(code, name)
WHERE c.code = 'INVOICE_SHIPMENT_LIMIT';


-- =========================
-- INVOICE_AMOUNT_LIMIT
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('120000', '10000'),
                         ('50000', '50000'),
                         ('Unlimited', 'Unlimited')
) v(code, name)
WHERE c.code = 'INVOICE_AMOUNT_LIMIT';

-- =========================
-- ACCOUNT_SEARCH_TYPE
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('CONTAINS', 'Contains'),
                         ('EXACT_MATCH', 'Exact Match')
) v(code, name)
WHERE c.code = 'ACCOUNT_SEARCH_TYPE';



-- =========================
-- SPLIT_ENTRY
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('BOE', 'BOE'),
                         ('PAYMENT_REFERENCED', 'Payment Referenced')
) v(code, name)
WHERE c.code = 'SPLIT_ENTRY';

-- =========================
-- POTENTIAL_DELAY
-- =========================

-- Generic insert helper pattern used below

INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         CROSS JOIN (VALUES
                         ('30 Days', '30 Days'),
                         ('45 Days', '45 Days'),
                         ('60 Days', '60 days')) v(code, name)
WHERE c.code = 'POTENTIAL_DELAY';

-- =========================
-- ACCOUNT SEARCH FIELD
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         JOIN (VALUES
                   ('ACCOUNT_NAME', 'Account Name'),
                   ('ACCOUNT_NUMBER', 'Account Number')
) v(code, name) ON TRUE
WHERE c.code = 'ACCOUNT_SEARCH_FIELD'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);


-- =========================
-- SERVICE_CODE
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         JOIN (VALUES
                   ('DUTY', 'Duty'),
                   ('VAT', 'Vat'),
                   ('REVENUE', 'Revenue'),
                   ('Offical Recipt', 'Offical Recipt')
) v(code, name) ON TRUE
WHERE c.code = 'SERVICE_CODE';


-- =========================
-- GLOBAL RULE (MERGED)
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active, load_data_from)
SELECT c.id, v.code, v.name, v.label, true, v.load_data_from
FROM category c
         JOIN (VALUES
                   ('G_PAYMENT_TYPE','paymentType','Payment Type',NULL),
                   ('G_THIRD_PARTY_ENTITY','thirdPartyEntity','Third Party Entity','Entity'),
                   ('G_PRODUCT_TYPE','productType','Product Type',NULL),
                   ('G_ORIGIN','shipperCountryCode','Origin','Country'),
                   ('G_DESTINATION','consigneeCountryCode','Destination','Country'),
                   ('G_COST_OF_GOODS','costOfGoods','Cost of Goods',NULL),
                   ('G_PRODUCT_GROUP','productGroup','Product Group',NULL),
                   ('G_CIF', 'costOfGoods', 'CIF', NULL)
) v(code, name, label, load_data_from) ON TRUE
WHERE c.code = 'GLOBAL_RULE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- =========================
-- CATEGORY TYPE (WITH EXEMPTED)
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         JOIN (VALUES
                   ('HV', 'HV', 'High Value Shipment'),
                   ('LV', 'LV', 'Low Value Shipment'),
                   ('Exempted', 'Exempted', 'Exempted Shipment')
) v(code, name, label) ON TRUE
WHERE c.code = 'CATEGORY_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);


-- =========================
-- PRODUCT TYPE (NEW)
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'G_PRODUCT_TYPE'
         JOIN (VALUES
                   ('RDG','RDG','RDG'),('RDL','RDL','RDL'),('RMY','RMY','RMY'),
                   ('RTC','RTC','RTC'),('RTD','RTD','RTD'),('RTI','RTI','RTI'),
                   ('RTN','RTN','RTN'),('RTP','RTP','RTP'),('LHS','LHS','LHS'),
                   ('DIM','DIM','DIM'),('CDX','CDX','CDX'),('DDX','DDX','DDX'),
                   ('EDX','EDX','EDX'),('MDX','MDX','MDX'),('PDX','PDX','PDX'),
                   ('PLX','PLX','PLX'),('SDX','SDX','SDX')
) v(code, name, label) ON TRUE
WHERE c.code = 'PRODUCT_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- =========================
-- PRODUCT GROUP
-- =========================

INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'G_PRODUCT_GROUP'
         JOIN (VALUES
                   ('EXP','EXP','EXP'),
                   ('DOM','DOM','DOM')
) v(code, name, label) ON TRUE
WHERE c.code = 'PRODUCT_GROUP'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- =========================
-- PAYMENT TYPE
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p ON p.code = 'G_PAYMENT_TYPE'
         JOIN (VALUES
                   ('3','3','3'),
                   ('P','P','P'),
                   ('F','F','F'),
                   ('C','C','C')
) v(code, name, label) ON TRUE
WHERE c.code = 'PAYMENT_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);



-- =========================
-- ELIGIBLE FOR BILLING
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         JOIN (VALUES
                   ('ESTIMATE','Estimate'),
                   ('ACTUAL','Actuals')
) v(code, name) ON TRUE
WHERE c.code = 'ELIGIBLE_FOR_BILLING'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- =========================
-- ACCOUNT
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         JOIN (VALUES
                   ('FIXED','FIXED'),
                   ('AMENDABLE','AMENDABLE')
) v(code, name) ON TRUE
WHERE c.code = 'ACCOUNT'
  AND NOT EXISTS (
    SELECT 1 FROM category_value cv WHERE cv.code = v.code
);

-- =========================
-- ACCOUNT STATUS
-- =========================
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.name, true
FROM category c
         JOIN (VALUES
                   ('ACTIVATED','Activated'),
                   ('DEACTIVATED','Deactivated'),
                   ('BLOCKED','Blocked')
) v(code, name) ON TRUE
WHERE c.code = 'ACCOUNT_STATUS';

-- =========================
-- Custom Serach
-- =========================

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('CHARGE_GROUP', 'Charge Group')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, NULL
FROM category c
         CROSS JOIN (
    VALUES
        ('COST', 'Cost', 'Cost'),
        ('CHARGE_REVENUE', 'Revenue', 'Revenue')
) v(code, name, label)
WHERE c.code = 'CHARGE_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);


INSERT INTO category_value
(category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p
              ON p.code = 'COST'
         JOIN (
    VALUES
        ('ECS', 'ECS', 'ECS'),
        ('ECS2', 'ECS2', 'ECS2'),
        ('EC3', 'EC3', 'EC3')
) v(code, name, label)
              ON TRUE
WHERE c.code = 'CHARGE_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);



INSERT INTO category_value
(category_id, code, name, label, is_active, parent_id)
SELECT c.id, v.code, v.name, v.label, true, p.id
FROM category c
         JOIN category_value p
              ON p.code = 'CHARGE_REVENUE'
         JOIN (
    VALUES
        ('DTA', 'DTA', 'DTA'),
        ('DTC', 'DTC', 'DTC'),
        ('DT3', 'DT3', 'DT3'),
        ('DTS', 'DTS', 'DTS')
) v(code, name, label)
              ON TRUE
WHERE c.code = 'CHARGE_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);


INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('CHARGE_STATUS', 'Charge Status')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('DUE', 'Due', 'Due'),
        ('PENDING_CALCULATION', 'Pending Calculation', 'Pending Calculation'),
        ('ACTUALIZED', 'ACTUALIZED', 'Actualized'),
        ('Exempted_status', 'Exempted', 'Exempted')
) v(code, name, label)
WHERE c.code = 'CHARGE_STATUS'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);


INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('ACCOUNT_TYPE', 'Account Type Code')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);

INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('CASH', 'Cash', 'Cash'),
        ('ACCOUNT', 'Account', 'Account')
) v(code, name, label)
WHERE c.code = 'ACCOUNT_TYPE'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);


INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('CUSTOM_SEARCH_CRITERIA', 'Custom Search Criteria')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('TRACKING_NUMBER', 'Tracking Number', 'Tracking Number'),
        ('CONSOLE', 'Console', 'Console'),
        ('BOE_SC', 'BOE', 'BOE'),
        ('ACCOUNT_NUMBER_SC', 'Account Number', 'Account Number')
) v(code, name, label)
WHERE c.code = 'CUSTOM_SEARCH_CRITERIA'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('SEARCH_CRITERIA_DATE_TYPE', 'Search Criteria Date Type')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('PICKUP_DATE', 'Pickup Date', 'Pickup Date'),
        ('ALERTING_DATE', 'Alerting Date', 'Alerting Date')
) v(code, name, label)
WHERE c.code = 'SEARCH_CRITERIA_DATE_TYPE'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);



-- =========================
-- DECIMAL_CONFIGURATION CATEGORY
-- =========================

INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('DECIMAL_CONFIGURATION', 'Decimal Configuration')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);

-- =========================
-- DECIMAL_CONFIGURATION VALUES
-- =========================

INSERT INTO category_value
(category_id, entity_code, code, name, label, is_active)
SELECT c.id, v.entity_code, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('RUH', 'DECIMAL_RUH', '2', '2'),
        ('JED', 'DECIMAL_JED', '2', '2'),
        ('DHA', 'DECIMAL_DHA', '2', '2')
) v(entity_code, code, name, label)
WHERE c.code = 'DECIMAL_CONFIGURATION'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);






INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('ACTUAL_UPLOAD_COUNT', 'Actual Upload Count')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, entity_code, code, name, label, is_active)
SELECT c.id, v.entity_code, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        (null, 'ACTUAL_UPLOAD_COUNT', '3', '3')
) v(entity_code, code, name, label)
WHERE c.code = 'ACTUAL_UPLOAD_COUNT'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);



INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('ESTIMATE_MANDATORY_COLUMNS', 'Estimate Mandatory Columns')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);


INSERT INTO category_value
(category_id, entity_code, code, name, label, is_active)
SELECT c.id, v.entity_code, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('RUH', 'Estimates_Template_MANDATORY_COLUMNS_RUH', 'AWB', 'Estimates_Template_MANDATORY_COLUMNS_RUH'),
        ('JED', 'Estimates_Template_MANDATORY_COLUMNS_JED', 'AWB', 'Estimates_Template_MANDATORY_COLUMNS_RUH'),
        ('DHA', 'Estimates_Template_MANDATORY_COLUMNS_DHA', 'AWB', 'Estimates_Template_MANDATORY_COLUMNS_RUH')
) v(entity_code, code, name, label)
WHERE c.code = 'ESTIMATE_MANDATORY_COLUMNS'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);


INSERT INTO category (code, name, is_active)
SELECT v.code, v.name, true
FROM (VALUES
          ('ACTUALS_MANDATORY_COLUMNS', 'Actuals Mandatory Columns')
     ) v(code, name)
WHERE NOT EXISTS (
    SELECT 1
    FROM category c
    WHERE c.code = v.code
);

INSERT INTO category_value
(category_id, entity_code, code, name, label, is_active)
SELECT c.id, v.entity_code, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (
    VALUES
        ('RUH', 'Actual_Template_MANDATORY_COLUMNS_RUH', 'AWB,Saddad', 'Actual_Template_MANDATORY_COLUMNS_RUH'),
        ('JED', 'Actual_Template_MANDATORY_COLUMNS_JED', 'AWB,Saddad', 'Actual_Template_MANDATORY_COLUMNS_RUH'),
        ('DHA', 'Actual_Template_MANDATORY_COLUMNS_DHA', 'AWB,Saddad', 'Actual_Template_MANDATORY_COLUMNS_RUH')
) v(entity_code, code, name, label)
WHERE c.code = 'ACTUALS_MANDATORY_COLUMNS'
  AND NOT EXISTS (
    SELECT 1
    FROM category_value cv
    WHERE cv.code = v.code
);

INSERT INTO category (code, name, is_active) values ('SYSTEM_RULES', 'System Rules', true);
INSERT INTO category_value (category_id, code, name, label, is_active)
SELECT c.id, v.code, v.name, v.label, true
FROM category c
         CROSS JOIN (VALUES
                         ('SHIPMENT_FILTER', 'SHIPMENT_FILTER','Shipment Filter Rule')
) v(code, name, label)
WHERE c.code = 'SYSTEM_RULES';
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
) VALUES (
             1,
             true,
             '2026-05-19 09:55:08.473',
             '2026-05-19 09:55:08.473',
             gen_random_uuid(),
             'Customs Type',
             'SHIPMENT_FILTER',
             'SYSTEM',
             'SA',
             'package rules
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
          ',
             'SH013 && DestinationEntity in (RUH, JED, DHA) && (EXP || (DOM && 72104493))',
             NULL,
             'SYSTEM'
         );

INSERT INTO charge_type_doc_id_mapping (
    id,
    created_by,
    updated_by,
    created_time,
    updated_time,
    charge_type,
    doc_id
)
SELECT
    gen_random_uuid(),
    'SYSTEM',
    'SYSTEM',
    NOW(),
    NOW(),
    v.charge_type,
    v.doc_id
FROM (VALUES
    ('DTC', '95'),
    ('DTA', '96'),
    ('ECD', '97'),
    ('DTS', '98'),
    ('DT3', '99'),
    ('ECS', '100'),
    ('EC3', '101'),
    ('DTN', '102'),
    ('DAN', '103'),
    ('CEN', '104'),
    ('DSN', '105'),
    ('DPN', '106'),
    ('EON', '107'),
    ('ETN', '108')
) AS v(charge_type, doc_id)
WHERE NOT EXISTS (
    SELECT 1
    FROM charge_type_doc_id_mapping existing
    WHERE existing.charge_type = v.charge_type
);
