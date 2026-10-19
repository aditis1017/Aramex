# Test Case.xlsx — column schema (canonical)

**Template file (do not change headers):**  
[../Sample File Reference/Test Case.xlsx](../Sample%20File%20Reference/Test%20Case.xlsx)

Copy this template to the export path before writing data rows. **Row 1 = headers.** Data starts at **row 2**.

## Columns (order fixed)

| # | Column | Required | Rules |
|---|--------|----------|--------|
| 1 | Screen Name | Yes | Module/screen from story (e.g. `Global Settings`, `AP Management`) |
| 2 | JIRA User Story ID | Yes | Parent story key `CCIPH-###` |
| 3 | JIRA Test Case ID | Yes after Jira | Sub-task key `CCIPH-####` (empty only during draft preview) |
| 4 | JIRA Test Case Title | Yes | Same as Jira sub-task summary (without `[TC_ID]` prefix optional) |
| 5 | JIRA Test Case Description | Yes | Objective + preconditions summary (1–3 sentences) |
| 6 | Test Case Scenario | Yes | High-level scenario name |
| 7 | Test Steps | Yes | Numbered steps; one step per line inside the cell |
| 8 | Expected Result | Yes | Single overall expected outcome |
| 9 | Priority | Yes | `High` \| `Medium` \| `Low` (match team Jira names) |
| 10 | Severity | Yes | `Critical` \| `Major` \| `Moderate` \| `Minor` |
| 11 | **Test Case Type** | Yes | Exactly one value from [allowed types](#test-case-type-allowed-values) |
| 12 | Automation (Y/N) | Yes | `Y` or `N` |
| 13 | Automation Execution Status | No | Blank until automated; then `Not Run` \| `Pass` \| `Fail` |
| 14 | Automation Remarks | No | Blank or notes |
| 15 | Functional (Pass/Fail) | No | Blank until manual execution |
| 16 | Functional Remarks | No | Blank or execution notes |

Trailing empty columns in the template may exist — leave them blank.

## Test Case Type (allowed values)

Use **exactly one** per row (pick the **primary** intent of the case):

| Value | When to use |
|-------|-------------|
| **Functional Testing** | UI/API behavior, validations, business rules, CRUD |
| **Integration Testing** | Cross-module, SAP/Salesforce/Boomi, file upload → persistence, multi-service flows |
| **Regression Testing** | Re-verify fixed/previously working behavior; suite-style checks |
| **Performance Testing** | Load, latency, bulk upload volume, response time thresholds |
| **Security Testing** | AuthZ/RBAC, Entra login, data exposure, injection, session |
| **Accessibility Testing** | WCAG, keyboard nav, screen reader labels, contrast |
| **Usability Testing** | UX clarity, error messages, workflow efficiency (not pure functional assert) |

Align coverage with [.cursor/rules/cci-testcase.mdc](../../rules/cci-testcase.mdc) (Functional, Integration, Regression, Performance, Security, Accessibility, Usability).

### Distribution guidance (per story)

| Scope | Minimum mix |
|-------|-------------|
| `smoke` | Mostly Functional; at least 1 Regression if story touches shared module |
| `regression` | Functional + Regression; Integration where story has integrations |
| `full` | At least one case each for types relevant to the story (do not invent Performance/Security without AC or architecture signal) |

## TC_ID convention (internal + Jira summary)

Use module-aware IDs consistent with Katalon:

```text
TC_<MODULE_ABBR>_<CATEGORY>_<NNN>
```

Examples: `TC_GS_FUNC_001`, `TC_APM_NEG_003`, `TC_UPL_INT_002`

- **Summary on Jira:** `[TC_GS_FUNC_001] Verify rule order saves successfully`

## Export path

```text
qa/manual-test-cases/exports/xlsx/<ScreenSlug>_CCIPH-###_<YYYY-MM-DD>.xlsx
```

Also write a CSV mirror (same columns) for diff/review:

```text
qa/manual-test-cases/exports/xlsx/<ScreenSlug>_CCIPH-###_<YYYY-MM-DD>.csv
```

## Populate xlsx

1. `Copy-Item` template to export path.
2. Run [qa/manual-test-cases/scripts/export-test-case-xlsx.ps1](../../../qa/manual-test-cases/scripts/export-test-case-xlsx.ps1) with the CSV mirror, **or** fill via Excel COM when script succeeds.
3. If script fails, deliver CSV + template path and note manual paste into row 2+.
