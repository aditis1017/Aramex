# Jira execution result comment templates

Used by **qa-jira-suite-result-updater** when posting PASS / FAIL / SKIP comments on the **Jira Test Case** issue via `addCommentToJiraIssue` (`contentFormat: markdown`).

Replace placeholders from the Katalon suite report bundle and the run context (`SITE_URL`, keys). Redact secrets.

| Placeholder | Meaning |
|-------------|---------|
| `<KATALON_TC_NAME>` | Katalon test case leaf name |
| `<SUITE_NAME>` | Test suite name |
| `<TIMESTAMP>` | Execution timestamp from report |
| `<DURATION>` | Duration in seconds |
| `<BROWSER>` / `<OS>` / `<VERSION>` | Environment from JUnit properties |
| `<REPORT_RELATIVE_PATH>` | Relative path to report folder |
| `<SITE_URL>` | Resolved Atlassian site base (no trailing slash) |
| `<BUG_KEY>` / `<TC_KEY>` | Jira issue keys for this run |

---

## PASS

```markdown
## Automation Result: PASS

| Field | Value |
|-------|-------|
| **Result** | PASS |
| **Katalon TC** | <KATALON_TC_NAME> |
| **Test Suite** | <SUITE_NAME> |
| **Executed** | <TIMESTAMP> |
| **Duration** | <DURATION>s |
| **Environment** | <BROWSER> / <OS> / Katalon <VERSION> |
| **Report** | `<REPORT_RELATIVE_PATH>/JUnit_Report.xml` |

---
*Synced via qa-jira-suite-result-updater*
```

---

## FAIL

```markdown
## Automation Result: FAIL

| Field | Value |
|-------|-------|
| **Result** | FAIL |
| **Katalon TC** | <KATALON_TC_NAME> |
| **Test Suite** | <SUITE_NAME> |
| **Executed** | <TIMESTAMP> |
| **Duration** | <DURATION>s |
| **Environment** | <BROWSER> / <OS> / Katalon <VERSION> |
| **Report** | `<REPORT_RELATIVE_PATH>/JUnit_Report.xml` |
| **Screenshots** | <COUNT or paths> |

### Root cause
```
<FAILURE_MESSAGE_FIRST_LINES>
```

### Failed step / keyword
`<FAILED_KEYWORD_OR_STEP>`

### Defect
- **Bug:** <BUG_KEY or "Pending — will be logged in this run">
- **Link:** <SITE_URL>/browse/<BUG_KEY>

---
*Synced via qa-jira-suite-result-updater*
```

If the bug is created **after** this comment, post a follow-up comment:

```markdown
## Defect linked

Automation failure for **<KATALON_TC_NAME>** is tracked in **<BUG_KEY>**.
<link: <SITE_URL>/browse/<BUG_KEY>>
```

---

## SKIP

```markdown
## Automation Result: SKIP

| Field | Value |
|-------|-------|
| **Result** | SKIP |
| **Katalon TC** | <KATALON_TC_NAME> |
| **Test Suite** | <SUITE_NAME> |
| **Executed** | <TIMESTAMP> |
| **Report** | `<REPORT_RELATIVE_PATH>/JUnit_Report.xml` |

**Note:** No defect logged for skipped tests.

---
*Synced via qa-jira-suite-result-updater*
```

---

## Parent Story suite summary (optional — once per suite / story if any FAIL)

```markdown
## Suite execution summary

**Suite:** <SUITE_NAME>
**Report:** `<REPORT_RELATIVE_PATH>/JUnit_Report.xml`
**Totals:** Pass <N_PASS> | Fail <N_FAIL> | Skip <N_SKIP>

### Failed test cases
| Jira TC | Katalon TC | Bug |
|---------|------------|-----|
| <TC_KEY> | <KATALON_TC_NAME> | <BUG_KEY> |

---
*Synced via qa-jira-suite-result-updater*
```
