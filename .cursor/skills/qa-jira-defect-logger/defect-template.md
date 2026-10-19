# Jira Defect Template (Automation Failures)

Use this template when filling the `description` field for bugs created by **qa-jira-defect-logger**. Replace placeholders with values from the Katalon failure bundle.

---

## Description structure

```markdown
## Summary
[1–2 sentences: what failed, where, and user-visible impact]

## Automation Details

| Field | Value |
|-------|-------|
| **Test Case** | TC_REG_001 |
| **Test Suite** | TS_REG_001-Suite |
| **Related Story** | PROJ-123 |
| **Execution Time** | 2026-04-24T06:59:16Z |
| **Duration** | 37.8s |
| **Katalon Version** | 11.1.1.0 |

## Failure Details

**Root cause:**
```
Login validation failed. Login fields still present. Current URL: https://accounts.example.com/login
```

**Failed step / keyword:**
`AuthKeywords.loginWithValidCredentials`

**Stack trace (truncated):**
```
com.kms.katalon.core.exception.StepFailedException: loginWithValidCredentials failed
    at AuthKeywords.loginWithValidCredentials(AuthKeywords.groovy:91)
    at TC_REG_001.run(TC_REG_001:14)
```

## Environment

| Field | Value |
|-------|-------|
| **OS** | Windows 11 64bit |
| **Browser** | Chrome 147.0.7727.102 |
| **Host** | Aditi Singh - TTNPL-Aditi |
| **Project** | KAT |

## Steps to Reproduce (from automation)

1. Open application at configured base URL
2. Navigate to login page
3. Enter valid credentials
4. Submit login form
5. **Expected:** User is redirected to dashboard
6. **Actual:** Login fields remain visible; user stays on login page

## Expected vs Actual

| | |
|---|---|
| **Expected** | User is logged in and redirected away from the login page |
| **Actual** | Login validation failed; login form still displayed |

## Evidence

- **Report path:** `Reports/20260424_122911/Regression/TS_REG_001-Suite/20260424_122911/JUnit_Report.xml`
- **Log files:** `execution0.log`, `console0.log`
- **Screenshots:** 8 attachments in report folder (see `attachments` property in JUnit report)

## Additional Notes

- [Any user-provided context, flakiness notes, or environment-specific observations]
- [Regression note if applicable: "May be related to PROJ-789 resolved on 2026-03-01"]

---
*Created via qa-jira-defect-logger from Katalon automation*
```

---

## Summary line format

Use this pattern for the Jira **summary** field:

```
[Automation] <TC_ID>: <short root cause in plain language>
```

**Good examples:**
- `[Automation] TC_REG_001: Login validation failed after submit`
- `[Automation] TC_CHECKOUT-003: Order total mismatch on payment page`
- `[Automation] TC_API_012: 500 response from /api/v1/users`

**Bad examples:**
- `Test failed` (too vague)
- `TC_REG_001 FAILED` (no cause)
- Full stack trace in summary (too long)

---

## Labels (recommended)

Pass in `additional_fields` when creating the issue:

```json
{
  "labels": ["automation", "katalon", "TC_REG_001"],
  "priority": { "name": "Medium" }
}
```

Adjust priority:
- **High** — critical path / P0 test case / blocks release
- **Medium** — regression suite failure with clear app defect
- **Low** — flaky test, environment/data issue suspected

---

## Rules

1. **Redact secrets** — never paste passwords, API tokens, or auth headers from logs
2. **Truncate stack traces** — include top 5–10 relevant lines, not the full log
3. **One bug per distinct root cause** — if multiple TCs fail for the same reason, one bug with all TC IDs listed
4. **Link to story** — always set `Related Story` and create a `Relates` issue link
5. **Use report evidence** — do not guess root cause beyond what logs show

---

## Quality gate (before creating Jira issue)

- [ ] Summary follows `[Automation] <TC_ID>: <cause>` format
- [ ] Root cause quoted from failure message or log
- [ ] Environment table filled from JUnit properties
- [ ] Expected vs Actual is clear and testable
- [ ] No credentials or PII in description
- [ ] Related story key included (or user confirmed none)
- [ ] Duplicate check completed
- [ ] Report path included for reproducibility
- [ ] **Phase 4C:** log error comment posted on bug (`log-error-comment-template.md`)
