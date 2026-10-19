# QA Test Case Template (CSV)

This document is the locked template for **qa-test-case-writer**.  
**Primary deliverable is a `.csv` file only** — do not use markdown tables as the saved suite format.

Canonical column layout: [SampleTemplate - Sheet1.csv](SampleTemplate%20-%20Sheet1.csv)

---

## CSV Column Definitions (exact order)

| # | Column | Required | Description |
|---|--------|----------|-------------|
| 1 | **Screen Name** | Yes | UI screen / module under test (e.g. `AP Management`, `B2B Keywords Manager`, `Charges Dashboard`) |
| 2 | **User Story ID** | Yes | Parent Jira story key (e.g. `CCIPH-200`) |
| 3 | **ID** | Yes | Unique test case ID (e.g. `TC_APM_001`, `TC_B2B_FUNC_001`) |
| 4 | **Test Case Description** | Yes | Short title / what is being validated |
| 5 | **Test Case Procedure** | No | Optional high-level procedure summary; leave blank if steps are self-explanatory |
| 6 | **Test Steps** | Yes | Numbered actionable steps (one action per line inside the cell) |
| 7 | **Acceptance Criteria** | Prefer | Linked AC text or AC ID from the story (e.g. `AC1 - Create AP Account`) |
| 8 | **Expected Result** | Yes | **Exactly one** observable outcome (single sentence) |
| 9 | **Priority** | Yes | `High`, `Medium`, or `Low` |
| 10 | **Department** | Yes | Default `QA` unless specified |
| 11 | **Automation (Y/N)** | Yes | `Yes` or `No` |
| 12 | **Automation Execution Status** | No | e.g. `Not Started`, `Post MVP`, `Ready`, `Automated` — blank if unknown |
| 13 | **Automation Remarks** | No | Notes for automation; blank if none |
| 14 | **(Functional) Pass/Fail** | No | Leave blank for new suites (manual execution result) |
| 15 | **Functional Remarks** | No | Leave blank for new suites |
| 16 | **Remarks.1** | No | Leave blank unless needed |
| 17 | **Assigned to** | No | Leave blank unless specified |
| 18 | **Status** | No | Leave blank for new suites (or `New` if team convention requires) |
| 19 | **Comments** | No | Leave blank unless needed |

Header row must match the sample file exactly (including the quoted `(Functional)\nPass/Fail` column name if writing CSV that Excel opens like the sample).

---

## Sample header (copy exactly)

```csv
Screen Name,User Story ID,ID,Test Case Description,Test Case Procedure,Test Steps,Acceptance Criteria,Expected Result,Priority,Department,Automation (Y/N),Automation Execution Status,Automation Remarks,"(Functional)
Pass/Fail",Functional Remarks,Remarks.1,Assigned to,Status,Comments
```

---

## Sample data row (from SampleTemplate)

| Field | Example |
|-------|---------|
| Screen Name | B2B Keywords Manager |
| User Story ID | CCIPH-226 |
| ID | TC_B2B_FUNC_001 |
| Test Case Description | Validate B2B Keywords Manager option is visible in left menu for Super Admin |
| Test Case Procedure | *(empty)* |
| Test Steps | `1. Login as Super Admin`<br>`2. Observe left-hand navigation menu`<br>`3. Verify menu placement` |
| Acceptance Criteria | *(empty or AC text)* |
| Expected Result | “B2B Keywords Manager” option should be visible above “Role and Permissions” |
| Priority | High |
| Department | QA |
| Automation (Y/N) | Yes |
| Automation Execution Status | Post MVP |

---

## Rules (Mandatory)

### 1. CSV only

- Save the suite as a **`.csv`** file under `TestCases/`.
- Do **not** create Jira Test Case issues as part of this skill (that is `/qa-manual-test-case`).
- Do **not** save markdown tables as the primary deliverable (chat may show a short summary only).

### 2. One Expected Result per test case

- Each row has **exactly one** `Expected Result`.
- No bullet lists or multi-outcome blocks in `Expected Result`.
- If multiple outcomes must be proven, create **separate rows** (separate IDs).

**Bad:** Detail page opens; fields are read-only; save succeeds.  
**Good:** User is navigated to the AP Account Detail page for the clicked Account Number.

### 3. Steps must be actionable

- Every step is a concrete UI/API action: navigate, click, enter, select, upload, assert visible text/state.
- Avoid vague steps (“verify it works”, “check the page”).
- Name screen controls using labels from AC / Figma / product context when available.
- In CSV cells, put each numbered step on its own line inside the quoted field (same as SampleTemplate).

### 4. Traceability

- `User Story ID` is required on every row.
- Prefer filling `Acceptance Criteria` from the story AC when available.
- Do not invent screens or controls not supported by requirements / Figma / architecture / Dev UI.

### 5. Coverage (aligned with qa-manual-test-case UI design)

For each story / feature (scope permitting):

| Type | Purpose | Map into CSV |
|------|---------|--------------|
| **Positive** | Happy path | Normal row; Priority High/Medium |
| **Negative** | Invalid / missing / unauthorized — assert **UI-visible** failure | Separate row; single ER = validation message / disabled action / no record created |
| **Boundary** | Min/max length, empty results, date bounds | Separate row |

- Smoke: at least 1 positive critical path.
- Regression: positive + negative + boundary where applicable.
- Full: regression + integration/edge/security where in scope.
- At least **1 negative** and **1 boundary** per feature when applicable.

### 6. ID naming

- Prefer module initials from story title: e.g. `AP Management | Create New AP Account` → `TC_APM_001`, `TC_APM_002`, …
- Or functional prefix from sample style: `TC_B2B_FUNC_001`.
- Fallback: `TC-<FEATURE>-<NNN>` (uppercase, no spaces).
- IDs must be unique within the CSV.

### 7. Priority

Use **High**, **Medium**, or **Low** (match SampleTemplate — not P0/P1).

### 8. Automation flags

- Set `Automation (Y/N)` to `Yes` for stable UI journeys; `No` for exploratory / one-off / environment-blocked cases.
- Leave Pass/Fail and execution remarks blank for newly generated suites unless the user asks to fill them.

---

## CSV writing conventions

1. UTF-8 encoding (no BOM preferred unless Excel requires BOM).
2. Quote any field that contains commas, quotes, or newlines.
3. Escape double quotes inside fields by doubling them (`""`).
4. Keep multiline `Test Steps` inside one quoted cell (as in SampleTemplate).
5. One test case = **one CSV data row**.

---

## Checklist before saving

- [ ] Header matches SampleTemplate column order and names
- [ ] Every row has Screen Name, User Story ID, ID, Description, Test Steps, Expected Result, Priority, Department, Automation (Y/N)
- [ ] Every Expected Result is a single verifiable outcome
- [ ] No vague steps
- [ ] Positive / negative / boundary coverage per story (or N/A documented in Assumptions)
- [ ] IDs are unique
- [ ] File saved under `TestCases/<feature-slug>-test-cases.csv`
