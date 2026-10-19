# Manual Test Case — Jira Sub-task Template

Use this template for **every** manual test case sub-task created under a Jira user story.
Fill all sections. Do not leave placeholders in the final Jira description.

---

## Jira Sub-task Fields

| Jira field | Source | Rules |
|------------|--------|-------|
| **Summary** | `TC_ID` + Title | Format: `[TC-XXX-001] Short descriptive title` (max ~200 chars) |
| **Issue Type** | Fixed | `Sub-task` (or project equivalent from metadata) |
| **Parent** | User story key | e.g. `PROJ-123` |
| **Description** | Full template below | Markdown |
| **Priority** | Map from test case Priority | See priority mapping table |
| **Labels** | Generated | `manual-test`, `<TC_ID>`, `<FEATURE>` |
| **Components** | Optional | Copy from parent story if relevant |

### Priority mapping

| Test case Priority | Jira Priority |
|--------------------|---------------|
| P0 | Highest |
| P1 | High |
| P2 | Medium |
| P3 | Low |

If the project uses different priority names, resolve via `getJiraIssueTypeMetaWithFields` and map accordingly.

---

## Description Template (copy into Jira `description`)

```markdown
## Manual Test Case

| Field | Value |
|-------|-------|
| **TC_ID** | TC-XXX-001 |
| **Type** | Positive / Negative / Boundary |
| **Priority** | P0 / P1 / P2 / P3 |
| **Severity** | Critical / Moderate |
| **Requirement Ref** | PROJ-123 / Confluence: Feature Spec §2.1 |
| **Parent Story** | PROJ-123 — [Story title] |

---

### Objective

[One sentence: what behavior this test validates]

---

### Preconditions

- [System state, roles, data, environment]
- [Second precondition if needed]

---

### Test Data

| Input | Value |
|-------|-------|
| [Field / parameter] | [Value or N/A] |

---

### Test Steps

| Step | Action | Expected Result (step-level) |
|------|--------|------------------------------|
| 1 | [Imperative action on named UI/API element] | [Observable result after this step] |
| 2 | [Action] | [Result] |
| 3 | [Action] | [Result] |

---

### Expected Result (overall)

[Single verifiable outcome for the entire test case — must match qa-test-case-writer rules]

---

### Postconditions

- [System state after successful execution, or N/A]

---

### Traceability

- **User Story:** [PROJ-123](https://YOUR-COMPANY.atlassian.net/browse/PROJ-123)
- **Acceptance Criterion:** [AC text or number]
- **Confluence:** [Link if applicable]

---

*Generated via qa-jira-manual-test-subtasks*
```

---

## Summary Format Examples

| TC_ID | Summary |
|-------|---------|
| TC-LOGIN-001 | `[TC-LOGIN-001] Successful login with valid credentials` |
| TC-LOGIN-002 | `[TC-LOGIN-002] Login rejected with incorrect password` |
| TC-CHECKOUT-003 | `[TC-CHECKOUT-003] Payment fails when card is expired` |

---

## Rules (Mandatory)

1. **One sub-task per test case** — never combine multiple TC_IDs into one sub-task.
2. **One overall expected result** — same rule as [test-case-template.md](../qa-test-case-writer/test-case-template.md).
3. **Actionable steps** — each step names the UI element, field, API, or state explicitly.
4. **Traceability required** — every sub-task links back to the parent story and requirement source.
5. **No secrets in Jira** — redact passwords, tokens, API keys; use `[valid password]` or test-account references.
6. **Severity** — use `Critical` for P0 / happy-path blockers; `Moderate` for P1–P3 unless user specifies otherwise.

---

## Example — Filled Description

```markdown
## Manual Test Case

| Field | Value |
|-------|-------|
| **TC_ID** | TC-LOGIN-001 |
| **Type** | Positive |
| **Priority** | P0 |
| **Severity** | Critical |
| **Requirement Ref** | AUTH-101 / Confluence: Login §2.1 |
| **Parent Story** | AUTH-101 — User can log in with email and password |

---

### Objective

Verify an active user can log in with valid credentials and reach the dashboard.

---

### Preconditions

- User account `user@example.com` exists and is active
- Login page is reachable at `/login`

---

### Test Data

| Input | Value |
|-------|-------|
| Email | user@example.com |
| Password | [valid test account password] |

---

### Test Steps

| Step | Action | Expected Result (step-level) |
|------|--------|------------------------------|
| 1 | Open `/login` | Login form with **Email** and **Password** fields is displayed |
| 2 | Enter `user@example.com` in **Email** | Value is accepted in the field |
| 3 | Enter valid password in **Password** | Value is masked |
| 4 | Click **Sign in** | Page begins navigation |

---

### Expected Result (overall)

User is redirected to `/dashboard` and the header shows the account display name.

---

### Postconditions

- User session is active; logout is available from the header menu

---

### Traceability

- **User Story:** [AUTH-101](https://YOUR-COMPANY.atlassian.net/browse/AUTH-101)
- **Acceptance Criterion:** AC-1 — Registered user can sign in with email and password
- **Confluence:** Login Spec §2.1

---

*Generated via qa-jira-manual-test-subtasks*
```
