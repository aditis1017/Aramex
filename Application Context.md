Customs Charges Processing System (CCPS)
The Customs Charges Processing System (CCPS) is an enterprise logistics application developed to automate the processing, calculation, reconciliation, and invoicing of customs-related charges for shipment transactions.
The application serves as a centralized platform for managing customs charges across the shipment lifecycle and replaces manual spreadsheet-based calculations with a scalable, rule-driven, and auditable system.
CCPS enables operational and finance teams to process customs charges efficiently through automated business rules, event-driven workflows, and integration with enterprise financial systems such as SAP ERP.
The system supports both estimated and actual customs charge processing, allowing organizations to compare projected costs against finalized customs values and maintain financial consistency across logistics and finance operations.
The platform is designed to support:


Automated customs duty and charge calculations
Shipment-level customs processing
Rule-based charge determination
Estimate versus actual charge reconciliation
Invoice generation and management
Financial posting and synchronization
Audit tracking and historical record maintenance
Integration with external logistics and finance systems


The application processes shipment and customs events in real time or near real time and recalculates applicable charges based on configurable business rules and operational triggers.
CCPS acts as the operational customs processing layer while SAP remains the financial source of truth for accounting and financial reconciliation.
The system is used by multiple business users including operations teams, finance users, customs analysts, supervisors, and administrators to manage customs workflows, review charge calculations, validate invoices, and monitor financial consistency.
The application includes the following major functional areas:


Shipment Processing
Customs Charges Calculation Engine
Estimate vs Actual Processing
Invoicing
Financial Integration
Rules Configuration
Audit and Compliance Tracking
The platform is expected to handle high-volume shipment transactions with accuracy, traceability, scalability, and regulatory compliance while reducing manual effort and operational risk.



*/Program / product:/*
Customs Charges & Invoicing (Customs Charges Processing System) — automated calculation and management of customs charges at scale, with rule-based logic and financial integration.

*/Business intent:/* 
Replace manual, spreadsheet-based customs/charge calculations with a centralized, auditable, event-driven platform that supports estimates vs actuals, invoicing, and alignment with SAP as financial source of truth.

*/Primary user roles (by area):/* 
OPS and ADMIN across Rule Management, File Upload, Charges Dashboard, AP/AR Account Management, and Entity Management (per solution doc tables).


*/In-scope capabilities (functional):/*

Shipment ingestion from InfoAXS into this system
Charges upload (Excel via web) — Estimates, Actuals, Rate sheets
Charges calculation — estimates/actuals, variance/outstanding handling
Invoicing — generation, review, release toward SAP
GL/AP account handling with SAP and Salesforce sync patterns
Activity log / audit — user and time stamping; Javers-style object audit on key features
Logical applications / services to keep in mind when designing scenarios:

Shipment Service, Rule Management Service, Customs Charges Calculation Service, File Orchestration Service, Invoicing Service, Dashboards Service, Web UI, AP account sync (incl. AWS Lambda / schedules where described)
Key logistics / customs business constructs (test dimensions):

Incoterm-style treatment: DDU vs DDP (and DDO mentioned in shipment classification narrative) — global settings and manual switch flows (DDU↔DDP)
Shipment value / customer segmentation: LV / HV / B2B / Exempted categorization
Entity allocation at charge creation
Manual override paths for exceptions
Eligibility & validation: Shipment Service ingests InfoAXS (RabbitMQ) events; document calls out filtering for shipment type == FULL and CODE == "SH013" before Drools-driven eligibility/classification and persistence to CCI DB
Charge record types referenced in process steps (for scenario naming / data setup): e.g. ECD, DTC, DTA, ECM, DTS/DT3 (validate exact meanings in functional spec)
Representative end-to-end process (high level):

Charge creation at InfoAXS (OPS)
Charge creation / processing in Customs Charges & Invoicing (system)
Eligibility, shipment validation, business rules
Create charge records (with Salesforce / SAP touchpoints per process table)
Upload Estimates / Actuals via Excel
Compare Estimates vs Actuals
Review charge records (Invoicing)
Release charges to SAP
Generate invoice in SAP; capture document number
Integration & orchestration (for interface / E2E tests):

Boomi: API gateway, SAP (iDoc/BAPI), Salesforce (REST/Bulk), CCM flows, RabbitMQ / event streams as applicable
AWS: EKS microservices, RabbitMQ / Amazon MQ, SQS (incl. chunking, DLQ on failure), S3 (uploads, archives), EventBridge (scheduled triggers, e.g. AP sync), RDS PostgreSQL, ElastiCache/Redis (Valkey) in tech tables
Idempotency: natural key constraint (e.g. shipment_number + origin_system_id); account sync UPSERT semantics
Data domains useful for test data design: master data (entity, country, currency, exchange rates, categories), Shipments, Charges / Charge_Line_Item (multiple actuals per shipment possible), Invoices / line items, Accounts (AP/AR extensions), Rate_sheet / items, Drools global rules & DRL storage, uploaded_files and file_processing_errors, Javers tables, Spring Batch metadata where batch jobs exist.

Non-functional / operational expectations called out: 24/7 for core pipeline components (shipment, calculation, queues, DB/cache, file orchestration, rule UI, connectors subject to external maintenance windows); async durability when downstream ERP is degraded; dashboards for volume, success, pending charges, queue depth, alerts.

Controls & quality attributes to reflect in negative / security / audit scenarios: schema validation at entry, two-key governance for financial rule changes, RBAC / SoD (rule managers vs viewers), TLS in transit / encryption at rest, SAP as ledger source of truth, DLQ + UI remediation for failures, immutable audit trail (“golden thread”) from trigger to SAP document number.

Open design topics (flag as “assumption” or “pending” in tests): SAP/Salesforce/InfoAXS/CCM integration details, archival policy (e.g. retention discussion), SSO — called open in the architecture doc; confirm in functional spec before hard assertions.

Environments (for prompt scope): DEV, Staging, UAT, PROD — PROD isolated from other envs per tiering table.