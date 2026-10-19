# Jira Sub-task — CCI manual test case

Parent: **User Story** `CCIPH-###` (not Epic). One sub-task per test case.

## Jira fields

| Field | Value |
|-------|--------|
| Issue type | `Sub-task` (from `getJiraProjectIssueTypesMetadata`) |
| Parent | Story key via `createJiraIssue` `parent` parameter |
| Summary | `[<TC_ID>] <JIRA Test Case Title>` |
| Priority | Map from xlsx Priority (High→High, etc.) |
| Labels | `manual-test`, `<TC_ID>`, `tc-type-<slug>` e.g. `tc-type-functional` |
| Components | `Customs Charges UI` when available (same as qa-manual-test-case) |

### Test Case Type → label slug

| Test Case Type | Label |
|----------------|--------|
| Functional Testing | `tc-type-functional` |
| Integration Testing | `tc-type-integration` |
| Regression Testing | `tc-type-regression` |
| Performance Testing | `tc-type-performance` |
| Security Testing | `tc-type-security` |
| Accessibility Testing | `tc-type-accessibility` |
| Usability Testing | `tc-type-usability` |

## Description (markdown)

```markdown
## Manual Test Case (CCI)

| Field | Value |
|-------|-------|
| **TC_ID** | TC_GS_FUNC_001 |
| **Test Case Type** | Functional Testing |
| **Screen Name** | Global Settings |
| **Priority** | Medium |
| **Severity** | Moderate |
| **Parent Story** | CCIPH-30 |
| **Automation** | N |

### Objective
<JIRA Test Case Description>

### Scenario
<Test Case Scenario>

### Preconditions
- [Role, environment URL, data state]

### Test Steps
1. <step>
2. <step>

### Expected Result (overall)
<Expected Result>

### Traceability
- **Story:** CCIPH-30 — <summary>
- **Requirement source:** <sub-task or AC reference>
- **Architecture / context:** Application Context §…

---
*Generated via cci-manual-tc-subtask-xlsx*
```

Do not paste secrets. Use `http://dev-cc.aramex.com/customs-management` from product context for URLs.
