-- Core business tables aligned with JPA entities (PostgreSQL).
-- Runs after RBAC, JaVers, Spring Batch, and country/entity seed in the same v1.0.0 changelog.
-- =========================================
-- CURRENCY
-- =========================================

CREATE TABLE IF NOT EXISTS currency (
    id BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(10) NOT NULL,
    currency_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    country_code VARCHAR(100) NOT NULL,
    CONSTRAINT fk_currency_country_code FOREIGN KEY (country_code) REFERENCES country(code),
    CONSTRAINT uq_currency_country UNIQUE (country_code)
);

-- =========================================
-- AR / AP MASTER ACCOUNTS
-- =========================================

CREATE TABLE IF NOT EXISTS ar_account (
    id UUID PRIMARY KEY,
    entity_code VARCHAR(200) NOT NULL,
    account_name VARCHAR(250) NOT NULL,
    account_number VARCHAR(250) NOT NULL,
    country_code VARCHAR(50),
    ar_account_type VARCHAR(100),
    ar_account_number VARCHAR(100),
    status VARCHAR(100),
    team_id VARCHAR(100),
    team_name VARCHAR(255),
    reach_account_id VARCHAR(100),
    created_date TIMESTAMPTZ,
    created_by VARCHAR(100),
    CONSTRAINT uq_ar_account_number_entity UNIQUE (account_number, entity_code)
);

CREATE TABLE IF NOT EXISTS ap_account (
    id UUID PRIMARY KEY,
    entity_code VARCHAR(100) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(150) NOT NULL,
    market_structure VARCHAR(100),
    ap_account_status VARCHAR(50),
    account_type VARCHAR(50)
);

-- =========================================
-- CATEGORY
-- =========================================

CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS category_value (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    parent_id BIGINT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    label VARCHAR(200) NOT NULL,
    entity_code VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    load_data_from VARCHAR(255),
    CONSTRAINT fk_category_value_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_category_value_parent FOREIGN KEY (parent_id) REFERENCES category_value(id),
    CONSTRAINT uq_category_value_category_code UNIQUE (category_id, code)
);

-- =========================================
-- ENTRA GUID MAP
-- =========================================

CREATE TABLE IF NOT EXISTS entra_guid_entity_map (
    id BIGSERIAL PRIMARY KEY,
    guid VARCHAR(36) NOT NULL,
    entity_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(512),
    CONSTRAINT uq_entra_guid_entity_map_guid UNIQUE (guid)
);

-- =========================================
-- GLOBAL SETTINGS
-- =========================================

CREATE TABLE IF NOT EXISTS global_setting (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    destination_code VARCHAR(255),
    target_field VARCHAR(255),
    target_value VARCHAR(255),
    status BOOLEAN,
    sequence_number INTEGER,
    rule_signature VARCHAR(255),
    drl_rule TEXT,
    rule_condition TEXT
);

CREATE TABLE IF NOT EXISTS global_rule_criteria (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    global_setting_id UUID NOT NULL,
    field_name VARCHAR(255),
    operator VARCHAR(255),
    values VARCHAR(255),
    exact_value VARCHAR(255),
    CONSTRAINT fk_global_rule_criteria_setting FOREIGN KEY (global_setting_id) REFERENCES global_setting(id)
);

-- =========================================
-- KEYWORD RULES
-- =========================================

CREATE TABLE IF NOT EXISTS keyword_rules (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    keyword VARCHAR(50) NOT NULL,
    match_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN
);

-- =========================================
-- SHIPMENT / CHARGE
-- =========================================

CREATE TABLE IF NOT EXISTS shipment (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    attention_of VARCHAR(200),
    hawb_number VARCHAR(100),
    cash_currency VARCHAR(100),
    cash_value DOUBLE PRECISION,
    chargeable_weight DOUBLE PRECISION,
    payment_type VARCHAR(200),
    cod_currency VARCHAR(200),
    cod_value DOUBLE PRECISION,
    collect_amount DOUBLE PRECISION,
    collect_currency VARCHAR(200),
    collection_reference VARCHAR(100),
    commodity_country_code VARCHAR(100),
    commodity_description VARCHAR(500),
    consignee_id BIGINT,
    consignee_name VARCHAR(200),
    consignee_number VARCHAR(200),
    consignee_reference VARCHAR(255),
    consignee_reference2 VARCHAR(255),
    customs_currency VARCHAR(100),
    consignee_city VARCHAR(100),
    consignee_country_code VARCHAR(100),
    hawb_destination_entity VARCHAR(255),
    destination_location_code VARCHAR(100),
    consignee_state_code VARCHAR(100),
    consignee_zip_code VARCHAR(100),
    foreign_hawb_number VARCHAR(255),
    hawb_dimensions VARCHAR(255),
    hawbid BIGINT,
    insurance_currency VARCHAR(200),
    insurance_value DOUBLE PRECISION,
    origin_entity VARCHAR(200),
    shipper_city VARCHAR(200),
    shipper_country_code VARCHAR(100),
    origin_location_code VARCHAR(100),
    shipper_state_code VARCHAR(100),
    shipper_zip_code VARCHAR(200),
    pieces INTEGER,
    pickup_date TIMESTAMP,
    product_group VARCHAR(20),
    services VARCHAR(255),
    shipper_id BIGINT,
    shipper_name VARCHAR(200),
    shipper_number VARCHAR(200),
    shipper_reference VARCHAR(255),
    shipper_reference2 VARCHAR(255),
    skeleton BOOLEAN,
    third_party_entity VARCHAR(200),
    third_party_city VARCHAR(200),
    third_party_country VARCHAR(100),
    third_party_id BIGINT,
    third_party_name VARCHAR(200),
    third_party_number VARCHAR(200),
    third_party_reference VARCHAR(255),
    third_party_reference2 VARCHAR(255),
    third_party_state VARCHAR(20),
    hawb_alerted_date TIMESTAMP,
    product_type VARCHAR(50),
    unit VARCHAR(20),
    weight DOUBLE PRECISION,
    cost_of_goods DOUBLE PRECISION,
    cif_value DOUBLE PRECISION,
    cif_value_currency VARCHAR(50),
    business_type VARCHAR(50),
    pi_number VARCHAR(50),
    category_type VARCHAR(50),
    shipment_type VARCHAR(50),
    mawb_id BIGINT,
    mawb_console BIGINT,
    b2b_shipment BOOLEAN,
    actual_upload_count INTEGER,
    is_manual_shipment BOOLEAN,
    CONSTRAINT uk_shipment_hawb_number UNIQUE (hawb_number)
);

CREATE TABLE IF NOT EXISTS charge (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    shipment_id UUID,
    charge_code VARCHAR(255),
    entity_id BIGINT,
    status VARCHAR(255),
    estimate_amount NUMERIC(19, 2),
    actual_amount NUMERIC(19, 2),
    outstanding_amount NUMERIC(19, 2),
    currency VARCHAR(255),
    CONSTRAINT fk_charge_shipment FOREIGN KEY (shipment_id) REFERENCES shipment(id),
    CONSTRAINT fk_charge_entity FOREIGN KEY (entity_id) REFERENCES entity(id)
);

-- =========================================
-- ACCOUNT CONFIGURATION
-- =========================================

CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    entity_code VARCHAR(100) NOT NULL,
    account_nature VARCHAR(50) NOT NULL,
    ar_account_id UUID,
    ap_account_id UUID,
    split_entry VARCHAR(100),
    entry_limit VARCHAR(100),
    charge_type VARCHAR(50),
    sap_country_code VARCHAR(50),
    default_cash_account VARCHAR(50),
    split_invoice VARCHAR(50),
    invoice_limit VARCHAR(50),
    eligible_for_billing VARCHAR(50),
    invoice_trigger VARCHAR(50),
    rate_currency VARCHAR(100),
    billing_currency VARCHAR(100),
    account VARCHAR(100),
    default_account_number VARCHAR(100),
    default_ar_account_number VARCHAR(100),
    invoice_frequency VARCHAR(100),
    ar_invoice_frequency VARCHAR(100),
    ar_potential_delay VARCHAR(100),
    ar_invoice_amount_limit VARCHAR(100),
    ar_invoice_shipment_limit VARCHAR(100),
    ar_invoice_break_by VARCHAR(255),
    ar_business_type VARCHAR(100),
    ar_custom_type VARCHAR(50),
    ar_internal_account_number VARCHAR(100),
    ar_amount_code VARCHAR(100),
    version BIGINT,
    CONSTRAINT fk_account_entity_code FOREIGN KEY (entity_code) REFERENCES entity(code),
    CONSTRAINT fk_account_ar_account FOREIGN KEY (ar_account_id) REFERENCES ar_account(id),
    CONSTRAINT fk_account_ap_account FOREIGN KEY (ap_account_id) REFERENCES ap_account(id)
);

-- =========================================
-- SERVICE CODES / CHARGES
-- =========================================

CREATE TABLE IF NOT EXISTS service_codes (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    entity_code VARCHAR(100) NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    service_description VARCHAR(255),
    tax_code VARCHAR(50),
    is_deleted BOOLEAN,
    is_active BOOLEAN,
    fetch_tax BOOLEAN,
    type VARCHAR(255),
    charge_type VARCHAR(50),
    doc_type INTEGER,
    info_axs_id VARCHAR(255),
    CONSTRAINT fk_service_codes_entity FOREIGN KEY (entity_code) REFERENCES entity(code)
);

CREATE TABLE IF NOT EXISTS service_charge (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    account_id UUID,
    service_code_id UUID NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN,
    is_billable BOOLEAN,
    split VARCHAR(255),
    default_account_number VARCHAR(255),
    tax_value DOUBLE PRECISION,
    CONSTRAINT fk_service_charge_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_service_charge_service_code FOREIGN KEY (service_code_id) REFERENCES service_codes(id)
);

CREATE TABLE IF NOT EXISTS service_charge_mapping (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    from_service_code UUID NOT NULL,
    to_service_code UUID NOT NULL,
    from_entity VARCHAR(255) NOT NULL,
    to_entity VARCHAR(255) NOT NULL,
    charge_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_scm_from_service_code FOREIGN KEY (from_service_code) REFERENCES service_codes(id),
    CONSTRAINT fk_scm_to_service_code FOREIGN KEY (to_service_code) REFERENCES service_codes(id)
);

-- =========================================
-- RATE SHEETS
-- =========================================

CREATE TABLE IF NOT EXISTS rate_sheet (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN,
    account_id UUID NOT NULL,
    CONSTRAINT fk_rate_sheet_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS rate_sheet_item (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    rate_sheet_id UUID NOT NULL,
    description VARCHAR(255),
    service_code VARCHAR(255),
    shipment_type VARCHAR(255),
    is_deleted BOOLEAN,
    CONSTRAINT fk_rate_sheet_item_sheet FOREIGN KEY (rate_sheet_id) REFERENCES rate_sheet(id)
);

CREATE TABLE IF NOT EXISTS rate_sheet_item_rate (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    rate_sheet_item_id UUID NOT NULL,
    rate_type VARCHAR(255) NOT NULL,
    percentage_of VARCHAR(255),
    value NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_rate_sheet_item_rate_item FOREIGN KEY (rate_sheet_item_id) REFERENCES rate_sheet_item(id)
);

-- =========================================
-- BILLING / INVOICE
-- =========================================

CREATE TABLE IF NOT EXISTS billing_preferences (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    account_id UUID,
    charge_code VARCHAR(255),
    eligible_for_billing VARCHAR(255),
    invoice_trigger VARCHAR(255),
    split_invoice VARCHAR(255),
    invoice_limit INTEGER,
    invoicing_frequency VARCHAR(255),
    invoice_group_by VARCHAR(255),
    potential_delay_days INTEGER,
    CONSTRAINT fk_billing_preferences_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS charge_line_item (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    charge_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    account_id UUID,
    file_reference_id UUID,
    payment_reference VARCHAR(100),
    declaration_number VARCHAR(100),
    consol_group VARCHAR(100),
    paying_entity VARCHAR(100),
    service_code VARCHAR(255),
    exchange_rate DOUBLE PRECISION,
    info_axs_id VARCHAR(255),
    pre_taxable_amount NUMERIC(19, 2),
    pre_converted_amount NUMERIC,
    ap_account_number VARCHAR(100),
    CONSTRAINT fk_charge_line_item_charge FOREIGN KEY (charge_id) REFERENCES charge(id),
    CONSTRAINT fk_charge_line_item_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS invoice (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    invoice_type VARCHAR(255),
    invoice_number VARCHAR(255) UNIQUE,
    internal_reference VARCHAR(255),
    account_id UUID,
    entity_code VARCHAR(255),
    total_amount NUMERIC(19, 2),
    currency VARCHAR(255),
    document_date DATE,
    issue_date DATE,
    confirmed_at TIMESTAMPTZ,
    status VARCHAR(255),
    CONSTRAINT fk_invoice_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS invoice_line_items (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    invoice_id UUID,
    amount NUMERIC(19, 2),
    gl_account VARCHAR(255),
    company_code VARCHAR(255),
    CONSTRAINT fk_invoice_line_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id)
);

-- =========================================
-- FILE UPLOAD / PROCESSING
-- =========================================

CREATE TABLE IF NOT EXISTS staged_file_upload (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    upload_profile VARCHAR(128) NOT NULL,
    original_file_name VARCHAR(512) NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(4000),
    expected_data_chunks BIGINT,
    total_data_rows BIGINT,
    error_report_sent_at TIMESTAMPTZ,
    entity_code VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS processed_chunk (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    file_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uk_processed_chunk_file_chunk UNIQUE (file_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS custom_charge_file_error (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    file_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    chunk_number INTEGER NOT NULL,
    error_list JSONB NOT NULL
);

-- =========================================
-- SHIPMENT RULE ASSIGNMENT
-- =========================================

CREATE TABLE IF NOT EXISTS shipment_rule_assignment (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    hawb_number VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    rule_id UUID NOT NULL,
    domain_snapshot JSONB NOT NULL,
    rule_snapshot JSONB NOT NULL
);

-- =========================================
-- EXPORT JOB
-- =========================================

CREATE TABLE IF NOT EXISTS export_job (
    id UUID PRIMARY KEY,
    export_type VARCHAR(128) NOT NULL,
    format VARCHAR(16) NOT NULL,
    filter_spec TEXT,
    column_names_json TEXT,
    status VARCHAR(32) NOT NULL,
    total_records BIGINT,
    processed_records BIGINT NOT NULL DEFAULT 0,
    percent_complete NUMERIC(7, 2),
    correlation_id VARCHAR(128),
    requested_by VARCHAR(256),
    error_code VARCHAR(64),
    error_message VARCHAR(4000),
    s3_bucket VARCHAR(256),
    s3_key VARCHAR(1024),
    file_size_bytes BIGINT,
    content_type VARCHAR(128),
    local_temp_path VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

-- Business indexes
-- Indexes for core business tables (aligned with JPA @Index annotations).

CREATE INDEX IF NOT EXISTS idx_ar_account_entity_code ON ar_account (entity_code);

CREATE INDEX IF NOT EXISTS idx_ap_account_entity_code ON ap_account (entity_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ap_account_number_entity ON ap_account (account_number, entity_code);

CREATE INDEX IF NOT EXISTS idx_entra_guid_entity_map_guid_lower ON entra_guid_entity_map (LOWER(guid));

CREATE INDEX IF NOT EXISTS idx_keyword_rules_keyword ON keyword_rules (keyword);

CREATE INDEX IF NOT EXISTS idx_account_entity_code ON account (entity_code);
CREATE INDEX IF NOT EXISTS idx_account_nature ON account (account_nature);
CREATE INDEX IF NOT EXISTS idx_account_ar_master_charge ON account (ar_account_id, charge_type);
CREATE INDEX IF NOT EXISTS idx_account_ap_master_charge ON account (ap_account_id, charge_type);

CREATE INDEX IF NOT EXISTS idx_charge_line_item_charge_id ON charge_line_item (charge_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staged_file_upload_file_id ON staged_file_upload (file_id);

CREATE INDEX IF NOT EXISTS idx_processed_chunk_file_id ON processed_chunk (file_id);
CREATE INDEX IF NOT EXISTS idx_processed_chunk_file_completed ON processed_chunk (file_id, completed_at);

CREATE INDEX IF NOT EXISTS idx_shipment_rule_assignment_hawb ON shipment_rule_assignment (hawb_number);

CREATE INDEX IF NOT EXISTS idx_export_job_status_created ON export_job (status, created_at);
CREATE INDEX IF NOT EXISTS idx_export_job_type_created ON export_job (export_type, created_at);

CREATE TABLE IF NOT EXISTS charge_type_doc_id_mapping (
    id UUID PRIMARY KEY,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_time TIMESTAMPTZ NOT NULL,
    updated_time TIMESTAMPTZ NOT NULL,
    charge_type VARCHAR(10) NOT NULL,
    doc_id VARCHAR(10) NOT NULL,
    CONSTRAINT uq_charge_type_doc_id_mapping_charge_type UNIQUE (charge_type),
    CONSTRAINT uq_charge_type_doc_id_mapping_doc_id UNIQUE (doc_id)
);
