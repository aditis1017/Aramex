# Customs Charges — UI Product Context

Use this file as the **baseline UI product source** when generating UI test cases. Prefer live Jira acceptance criteria when present; use this context to ground screens, filters, roles, and journeys.

## Environments & Design

| Source | Value |
|--------|--------|
| Jira project | CCIPH |
| Jira board | https://aramex.atlassian.net/jira/software/c/projects/CCIPH/boards/1712 |
| Dev URL | http://dev-cc.aramex.com/customs-management |
| Figma prototype | https://taffy-legal-55683069.figma.site/ |
| Architecture / business document | `Customs Charges Application Solution Architecture_v2.2_8thMay.md` (repo root) |

## Web UI Modules (from Architecture)

| Module | Users | UI focus |
|--------|--------|----------|
| Charges Dashboard | OPS, ADMIN | Search/filter charges, view results, export |
| File Upload | OPS, ADMIN | Upload Actuals, Estimates, Rate Sheet (Excel); front-end template validation |
| Rule Management | OPS, ADMIN | Create / update / delete business rules (Drools) |
| Activity Logs | OPS, ADMIN | Javers audit trail — user + timestamp visibility |
| AP/AR Account Management | OPS, ADMIN | Account visibility / updates (scheduled sync) |
| Entity Management | OPS, ADMIN | Entity configuration |

## Charges Dashboard — UI controls (from Figma)

Observed on the CustomsCharges prototype:

**Header / actions**

- Greeting / logged-in user (e.g. Hello, Malek)
- Role indicator (e.g. Admin)
- Upload Charge
- Add New One
- Export to Excel
- Hide Details / show details toggle

**Primary filters**

| Control | Sample values |
|---------|----------------|
| Charge Country | All, Saudi Arabia, Jordan, UAE |
| Entity | All, Riyadh (RUH), Jeddah (JED), Dammam (DMM) |
| Charge Group | COST, REVENUE |
| Charge Code | ECS, EC3, ECD |
| Charge Status | All, Pending Calculation, DUE |
| Date Type | All, Pickup Date, Alerting Date |
| Start Date / End Date | Date pickers |
| Account Type | All, Cash, Account |
| Business Type | All, B2B, B2C |
| Category | All, Low Value, High Value, Exempted, B2B |

**Search criteria**

- Tracking Number
- Console
- BOE
- Account Number
- Multiple (bulk search)

**Additional details (when expanded)**

| Control | Sample values |
|---------|----------------|
| Origin Entity | Amman (AMM), Aqaba (AQB) |
| Origin Country | Jordan, Saudi Arabia |
| Dest. Entity | Amman (AMM), Aqaba (AQB) |
| Dest. Country | Jordan, Saudi Arabia |
| 3rd party Entity | Amman (AMM), Aqaba (AQB) |
| 3rd party Country | Jordan, Saudi Arabia |

**Primary CTA:** Search

## In-scope UI business capabilities

- Charges Upload (Excel via web app)
- Charges Dashboard search & display
- Review charge records
- Rule management via UI
- Activity log visualization
- Manual override / switch flows (DDU↔DDP) where exposed in UI
- RBAC: OPS vs ADMIN (and Business Viewer vs Rule Manager segregation)

## Roles & access (for UI security cases)

| Capability area | General users | Super users |
|-----------------|---------------|-------------|
| Rule Management | OPS | ADMIN |
| File Upload | OPS | ADMIN |
| Charges Dashboard | OPS | ADMIN |
| AP/AR Account Management | OPS | ADMIN |
| Entity Management | OPS | ADMIN |

Assert visible menus, enabled actions, and denied/hidden actions per role. Do not invent roles beyond retrieved Jira + this context.

## UI journeys to prefer when designing cases

1. Login → open Customs Management → Charges Dashboard → apply filters → Search → verify grid/results
2. Expand Additional Details → apply origin/dest/3rd-party filters → Search
3. Upload Charge (Estimates / Actuals / Rate Sheet) → validate template → success/error feedback
4. Add New One → complete form → save → verify record
5. Export to Excel → verify download / success feedback
6. Hide Details / show details → layout and filter visibility
7. Unauthorized / insufficient role → action hidden or blocked with clear UI message
