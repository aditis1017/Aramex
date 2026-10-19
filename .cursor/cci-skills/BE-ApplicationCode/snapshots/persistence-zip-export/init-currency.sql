-- ============================================
-- SCHEMA
-- ============================================



-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- CURRENCY TABLE
-- ============================================

DROP TABLE IF EXISTS  currency CASCADE;

CREATE TABLE  currency (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    currency_code varchar(10) NOT NULL,
    symbol varchar(20),
    currency_name varchar(150) NOT NULL,
    units varchar(100),
    iso_number integer,
    factor integer,

    created_date timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(255),
    updated_by varchar(255),

    CONSTRAINT currency_pkey PRIMARY KEY (id),
    CONSTRAINT currency_code_unique UNIQUE (currency_code)
);

CREATE INDEX idx_currency_code ON  currency(currency_code);
CREATE INDEX idx_currency_iso ON  currency(iso_number);

-- ============================================
-- CURRENCY EXCHANGE TABLE (DDL ONLY - NO DATA)
-- ============================================

-- DROP TABLE  currency_exchange;

CREATE TABLE  currency_exchange (
	id uuid NOT NULL,
	currency_id uuid NOT NULL,
	exchange_rate numeric(18, 6) NOT NULL,
	effective_date timestamp(6) NOT NULL,
	created_date timestamptz(6) NOT NULL,
	updated_date timestamptz(6) NOT NULL,
	created_by varchar(255) NULL,
	updated_by varchar(255) NULL,

	CONSTRAINT currency_exchange_pkey PRIMARY KEY (id),
	CONSTRAINT currency_exchange_rate_chk CHECK ((exchange_rate > (0)::numeric)),
	CONSTRAINT currency_exchange_unique UNIQUE (currency_id, effective_date),
	CONSTRAINT currency_exchange_fk FOREIGN KEY (currency_id)
		REFERENCES  currency(id) ON DELETE CASCADE
);

CREATE INDEX idx_currency_exchange_currency_date
ON  currency_exchange USING btree (currency_id, effective_date DESC);

CREATE INDEX idx_currency_exchange_effective_date
ON  currency_exchange USING btree (effective_date);

-- ============================================
-- SEED DATA (FULL CURRENCY LIST)
-- ============================================

INSERT INTO  currency (
    currency_code,
    symbol,
    currency_name,
    units,
    iso_number,
    factor,
    created_by,
    updated_by
)
VALUES
('AED','Dh','Dirham','fils',784,100,'SYSTEM','SYSTEM'),
('AFA','Af','Afghani','puls',971,100,'SYSTEM','SYSTEM'),
('ALL','L','Lek','qindarka',8,100,'SYSTEM','SYSTEM'),
('AMD','$','Dram','luma',51,100,'SYSTEM','SYSTEM'),
('ANG','$','Antillean Guilder','cents',532,100,'SYSTEM','SYSTEM'),
('AOA','Kz','Kwanza','lwei',973,100,'SYSTEM','SYSTEM'),
('AOK','Kz','Old Kwanza','lwei',973,100,'SYSTEM','SYSTEM'),
('ARS','A','Argentine Peso','centavos',32,100,'SYSTEM','SYSTEM'),
('ATS','S','Austrian Schilling','groschen',40,100,'SYSTEM','SYSTEM'),
('AUD','A$','Australian Dollar','cents',36,100,'SYSTEM','SYSTEM'),
('AWG','Af.','Aruban Florin','cents',533,100,'SYSTEM','SYSTEM'),
('AZN','N','Manat','qapik',944,100,'SYSTEM','SYSTEM'),
('BAD','$','Bosnian Dinar','para',977,100,'SYSTEM','SYSTEM'),
('BAM','KM','Bosnia-Herzegovina Convertible Marka','Fening',977,100,'SYSTEM','SYSTEM'),
('BBD','Bds$','Barbadian Dollar','cents',52,100,'SYSTEM','SYSTEM'),
('BDT','Tk','Taka','paisa',50,100,'SYSTEM','SYSTEM'),
('BEF','BF','Belgian Franc','centimes',56,100,'SYSTEM','SYSTEM'),
('BGL','Lv','Lev','stotinki',975,100,'SYSTEM','SYSTEM'),
('BGN','Lv','Lev','stotinki',975,100,'SYSTEM','SYSTEM'),
('BHD','BD','Bahraini Dinar','fils',48,1000,'SYSTEM','SYSTEM'),
('BIF','Fbu','Burundi Franc','centimes',108,100,'SYSTEM','SYSTEM'),
('BMD','Bd$','Bermudian Dollar','cents',60,100,'SYSTEM','SYSTEM'),
('BND','B$','Bruneian Dollar','sen',96,100,'SYSTEM','SYSTEM'),
('BOB','Bs','Boliviano','centavos',68,100,'SYSTEM','SYSTEM'),
('BRE','$','Cruzeiro','centavos',986,100,'SYSTEM','SYSTEM'),
('BRL','R$','Brazilian real','centavos',986,100,'SYSTEM','SYSTEM'),
('BTN','Nu','Ngultrum','chetrum',64,100,'SYSTEM','SYSTEM'),
('BWP','P','Pula','thebe',72,100,'SYSTEM','SYSTEM'),
('BZD','BZ$','Belizean Dollar','cents',84,100,'SYSTEM','SYSTEM'),
('CAD','Can$','Canadian Dollar','cents',124,100,'SYSTEM','SYSTEM'),
('CDF','FC','Congolese Franc','centimos',976,100,'SYSTEM','SYSTEM'),
('CHF','SwF','Swiss Franc','centimes',756,100,'SYSTEM','SYSTEM'),
('CID','CI$','Caymanian Dollar','cents',136,100,'SYSTEM','SYSTEM'),
('CLP','Ch$','Chilean Peso','centavos',152,100,'SYSTEM','SYSTEM'),
('CNY','Y','Yuan','jiao',156,100,'SYSTEM','SYSTEM'),
('COP','Col$','Colombian Peso','centavos',170,100,'SYSTEM','SYSTEM'),
('CRC','C','Costa Rican Colon','centimos',188,100,'SYSTEM','SYSTEM'),
('CUP','Cu$','Cuban Peso','centavos',192,100,'SYSTEM','SYSTEM'),
('CVE','$','Cape Verdean Escudo','centavos',132,100,'SYSTEM','SYSTEM'),
('CYP','£C','Cypriot Pound','cents',978,100,'SYSTEM','SYSTEM'),
('CZK','CK','Koruna','halers',203,100,'SYSTEM','SYSTEM'),
('DEM','DM','Deutsche Mark','pfennige',276,100,'SYSTEM','SYSTEM'),
('DJF','DF','Djibouti Franc','centimes',262,100,'SYSTEM','SYSTEM'),
('DKK','Dkr','Danish Krone','ore',208,100,'SYSTEM','SYSTEM'),
('DOP','RD$','Dominican Peso','centavos',214,100,'SYSTEM','SYSTEM'),
('DZD','DA','Algerian Dinar','centimes',12,100,'SYSTEM','SYSTEM'),
('EGP','£E','Egyptian Pound','piasters',818,100,'SYSTEM','SYSTEM'),
('ETB','Br','Birr','cents',230,100,'SYSTEM','SYSTEM'),
('EUR','€','Euro','cents',978,100,'SYSTEM','SYSTEM'),
('GBP','£','British Pound','pence',826,100,'SYSTEM','SYSTEM'),
('HKD','HK$','Hong Kong Dollar','cents',344,100,'SYSTEM','SYSTEM'),
('IDR','Rp','Indonesian Rupiah','sen',360,100,'SYSTEM','SYSTEM'),
('ILS','NIS','New Israeli Shekel','new agorot',376,100,'SYSTEM','SYSTEM'),
('INR','Rs','Indian Rupee','paise',356,100,'SYSTEM','SYSTEM'),
('IQD','ID','Iraqi Dinar','fils',368,1000,'SYSTEM','SYSTEM'),
('IRR','Rls','Iranian Rial',NULL,364,0,'SYSTEM','SYSTEM'),
('ISK','Ikr','Krona','aurar',352,100,'SYSTEM','SYSTEM'),
('ITL','Lit','Italian Lira','centesimi',380,100,'SYSTEM','SYSTEM'),
('JMD','J$','Jamaican Dollar','cents',388,100,'SYSTEM','SYSTEM'),
('JOD','JD','Jordanian Dinar','fils',400,1000,'SYSTEM','SYSTEM'),
('JPY','¥','Yen','sen',392,100,'SYSTEM','SYSTEM'),
('KES','K SH','Kenyan Shilling','cents',404,100,'SYSTEM','SYSTEM'),
('KGS','$','Som','tyyyn',417,100,'SYSTEM','SYSTEM'),
('KHR','CR','Riel','sen',116,100,'SYSTEM','SYSTEM'),
('KMF','CF','Comoran Franc','centimes',174,100,'SYSTEM','SYSTEM'),
('KPW','Wn','Won','chon',408,100,'SYSTEM','SYSTEM'),
('KRW','W','Won','chon',410,100,'SYSTEM','SYSTEM'),
('KWD','KD','Kuwaiti Dinar','fils',414,1000,'SYSTEM','SYSTEM'),
('KYD','CI$','Caymanian Dollar','cents',136,100,'SYSTEM','SYSTEM'),
('KZT','GEL','Tenge','tiyn',398,100,'SYSTEM','SYSTEM'),
('LAK','KN','New Kip','at',418,100,'SYSTEM','SYSTEM'),
('LBP','£L','Lebanese Pound','piastres',422,100,'SYSTEM','SYSTEM'),
('LKR','SLRs','Sri Lankan Rupee','cents',144,100,'SYSTEM','SYSTEM'),
('LRD','$','Liberian Dollar','cents',430,100,'SYSTEM','SYSTEM'),
('LSL','L','Loti','lisente',426,100,'SYSTEM','SYSTEM'),
('LTL','$','Litas','centu',440,100,'SYSTEM','SYSTEM'),
('LUF','LuxF','Luxembourg Franc','centimes',442,100,'SYSTEM','SYSTEM'),
('LVL','Ls','Lat','santims',428,100,'SYSTEM','SYSTEM'),
('LYD','LD','Libyan Dinar','dirhams',434,1000,'SYSTEM','SYSTEM'),
('MAD','DH','Moroccan Dirham','centimes',504,100,'SYSTEM','SYSTEM'),
('MDL','$','Leu',NULL,498,10,'SYSTEM','SYSTEM'),
('MGF','FMG','Ariary','centimes',969,100,'SYSTEM','SYSTEM'),
('MKD','$','Denar',NULL,807,10,'SYSTEM','SYSTEM'),
('MMK','K','Kyat','pyas',104,100,'SYSTEM','SYSTEM'),
('MNT','Tug','Tughrik','mongos',496,100,'SYSTEM','SYSTEM'),
('MOP','P','Pataca','avos',446,100,'SYSTEM','SYSTEM'),
('MRO','UM','Ouguiya','khoums',478,10,'SYSTEM','SYSTEM'),
('MUR','$','Mauritian Rupee','cents',480,100,'SYSTEM','SYSTEM'),
('MVR','Rf','Rufiyaa','lari',462,100,'SYSTEM','SYSTEM'),
('MWK','MK','Malawian Kwacha','tambala',454,100,'SYSTEM','SYSTEM'),
('MXN','Mex$','Nuevo Peso','centavos',484,100,'SYSTEM','SYSTEM'),
('MYR','RM','Ringgit','sen',458,100,'SYSTEM','SYSTEM'),
('MZN','MT','Mozambique New Metical','metical',0,100,'SYSTEM','SYSTEM'),
('NAD','$','Namibian Dollar','cents',516,100,'SYSTEM','SYSTEM'),
('NGN','N','Naira','kobo',566,100,'SYSTEM','SYSTEM'),
('NOK','NKr','Norwegian Krone','ore',578,100,'SYSTEM','SYSTEM'),
('NPR','NRs','Nepalese Rupee','paise',524,100,'SYSTEM','SYSTEM'),
('NZD','NZ$','New Zealand Dollar','cents',554,100,'SYSTEM','SYSTEM'),
('OMR','RO','Omani Rial','baizas',512,1000,'SYSTEM','SYSTEM'),
('PAB','B','Balboa','centesimos',590,100,'SYSTEM','SYSTEM'),
('PEN','S/.','Nuevo Sol','centimos',604,100,'SYSTEM','SYSTEM'),
('PGK','K','Kina','toeas',598,100,'SYSTEM','SYSTEM'),
('PHP','P','Philippine Peso','centavos',608,100,'SYSTEM','SYSTEM'),
('PKR','PRs','Pakistani Rupee','paisa',586,100,'SYSTEM','SYSTEM'),
('PLN','Z-I','Polish Zloty','groszy',985,100,'SYSTEM','SYSTEM'),
('PYG','G','Guarani','centimos',600,100,'SYSTEM','SYSTEM'),
('QAR','QR','Qatari Riyal','dirhams',634,100,'SYSTEM','SYSTEM'),
('RON','L','Leu','bani',946,100,'SYSTEM','SYSTEM'),
('RSD','RSD','Serbia Dinar','cents',941,100,'SYSTEM','SYSTEM'),
('RUB','BR','Ruble','kopecks',643,100,'SYSTEM','SYSTEM'),
('RWF','RF','Rwandan Franc','centimes',646,100,'SYSTEM','SYSTEM'),
('SAR','SRls','Saudi Riyal','halalas',682,100,'SYSTEM','SYSTEM'),
('SEK','Sk','Swedish Krona','ore',752,100,'SYSTEM','SYSTEM'),
('SGD','S$','Singapore Dollar','cents',702,100,'SYSTEM','SYSTEM'),
('SLL','Le','Leone','cents',694,100,'SYSTEM','SYSTEM'),
('SOS','$','Somali Shilling','centesimi',706,100,'SYSTEM','SYSTEM'),
('STD','Db','Dobra','centimos',678,100,'SYSTEM','SYSTEM'),
('SYP','£S','Syrian Pound','piastres',760,100,'SYSTEM','SYSTEM'),
('SZL','L','Lilangeni','cents',748,100,'SYSTEM','SYSTEM'),
('THB','Bht','Baht','sastangs',764,100,'SYSTEM','SYSTEM'),
('TND','TD','Tunisian Dinar','millimes',788,1000,'SYSTEM','SYSTEM'),
('TOP','PT','Paanga','seniti',776,100,'SYSTEM','SYSTEM'),
('TRY','TL','Turkish Lira TRY','kurus',949,100,'SYSTEM','SYSTEM'),
('TTD','TT$','Trinidad Dollar','cents',780,100,'SYSTEM','SYSTEM'),
('TWD','NT$','New Taiwan Dollar','cents',901,100,'SYSTEM','SYSTEM'),
('TZS','TSh','Tanzanian Shilling','cents',834,100,'SYSTEM','SYSTEM'),
('UAH','?','Hryvnia','kopeck',980,100,'SYSTEM','SYSTEM'),
('UGX','USh','Ugandan Shilling','cents',800,0,'SYSTEM','SYSTEM'),
('USD','$','United States Dollar','cents',840,100,'SYSTEM','SYSTEM'),
('UYU','$U','Uruguayan Peso','centesimos',940,100,'SYSTEM','SYSTEM'),
('UZS','Som','Uzbekistani Som','tiyin',0,100,'SYSTEM','SYSTEM'),
('VND','D','Viet Nam Dong','hao',704,10,'SYSTEM','SYSTEM'),
('VUV','VT','Vatu','centimes',548,100,'SYSTEM','SYSTEM'),
('XAF','CFAF','CFA Franc BEAC','centimes',950,100,'SYSTEM','SYSTEM'),
('XCD','EC$','East Caribbean Dollar','cents',951,100,'SYSTEM','SYSTEM'),
('XOF','CFAF','CFA Franc BCEAO','centimes',952,100,'SYSTEM','SYSTEM'),
('XPF','CFPF','CFP Franc','centimes',953,100,'SYSTEM','SYSTEM'),
('YER','YRls','Yemeni Rial','fils',886,100,'SYSTEM','SYSTEM'),
('ZAR','R','Rand','cents',710,100,'SYSTEM','SYSTEM'),
('ZMW','ZK','Zambian Kwacha','ngwee',421,100,'SYSTEM','SYSTEM'),
('ZWD','Z$','Zimbabwean Dollar','cents',932,100,'SYSTEM','SYSTEM');