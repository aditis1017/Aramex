---
name: cci-manual-tc-subtask-xlsx
description: >-
  Generates CCIPH manual UI test cases from Jira/Confluence and cci-skills context,
  creates each case as a Jira Sub-task under the parent User Story, and exports the
  same rows to Test Case.xlsx format. Uses Test Case Type (Functional, Integration,
  Regression, Performance, Security, Accessibility, Usability). Trigger on
  /cci-manual-tc-subtask-xlsx CCIPH-### or bare CCIPH key when this skill is attached.
---

# CCI Manual Test Cases — Jira Sub-tasks + Test Case.xlsx

Generate **manual test cases** for Customs Charges (CCIPH), publish **one Jira Sub-task per case** under the parent **User Story**, and export an **`.xlsx`** workbook matching the team template.

## Related rules & skills (read before generating)

| Resource | Use |
|----------|-----|
| [.cursor/rules/cci-testcase.mdc](../../rules/cci-testcase.mdc) | QA Lead coverage: functional, integration, regression, performance, security, accessibility, usability |
| [.cursor/skills/qa-manual-test-case/ui-product-context.md](../../skills/qa-manual-test-case/ui-product-context.md) | Dev URL, Figma, modules, roles |
| [../Required Files/Application Context.md](../Required%20Files/Application%20Context.md) | CCPS domain |
| [../Required Files/Customs Charges Application Solution Architecture_v2.2_8thMay.md](../Required%20Files/Customs%20Charges%20Application%20Solution%20Architecture_v2.2_8thMay.md) | Integrations, services |
| [../FE-ApplicationCode/docs/qa/README.md](../FE-ApplicationCode/docs/qa/README.md) | Route ↔ module mapping |
| [test-case-xlsx-schema.md](test-case-xlsx-schema.md) | Excel columns + **Test Case Type** values |
| [jira-subtask-template.md](jira-subtask-template.md) | Sub-task description layout |
| [qa-knowledge-retriever/SKILL.md](../../skills/qa-knowledge-retriever/SKILL.md) | Jira + Confluence fetch |
| [TempFolder/qa-jira-manual-test-subtasks/SKILL.md](../../skills/TempFolder/qa-jira-manual-test-subtasks/SKILL.md) | Sub-task duplicate check & `createJiraIssue` patterns |

**Do not** use `qa-manual-test-case` issue type **Test Case** + `createIssueLink` in this skill — here each case is a **Sub-task** with `parent = STORY_KEY`.

## Invocation

| User message | Action |
|--------------|--------|
| `/cci-manual-tc-subtask-xlsx CCIPH-30` | Full pipeline |
| `CCIPH-30` (skill attached) | Same |
| `Generate manual sub-tasks and xlsx for CCIPH-102` | Same |
| `draft only CCIPH-30` | Generate plan + CSV/xlsx **without** Jira create |

Default: **auto-create** Jira sub-tasks and xlsx (no Yes/Proceed), unless user says **draft only**.

| Parameter | Default |
|-----------|---------|
| Scope | `regression` (`smoke` \| `regression` \| `full`) |
| Project | `CCIPH` |
| Board | https://aramex.atlassian.net/jira/software/c/projects/CCIPH/boards/1712 |

---

## Phase 0 — Validate

- Story key must match `CCIPH-\d+`.
- Parent must be a **Story** (or equivalent that allows sub-tasks). If Epic, ask for a single story key.
- MCP **`user-atlassian`** authenticated.

---

## Phase 1 — Requirements (Agent-2)

1. `getAccessibleAtlassianResources` → `cloudId`.
2. `getJiraIssue` on `STORY_KEY`: summary, description, acceptance criteria, labels, components, attachments, comments, **subtasks**.
3. Fetch linked **Sub-tasks** on the story (requirement sources) via `parent = STORY_KEY` JQL — use for traceability, not as parent for new TC sub-tasks.
4. Confluence via remote links, doc-index, or `searchConfluence`.
5. Load cci-skills **Required Files** + **ui-product-context** + **FE** routes for the module (infer module from story title before `|`).
6. Produce: AC list, gaps, screen name, integration touchpoints.

Apply **cci-testcase.mdc** thinking: positive, negative, edge, regression impact — map each case to a **Test Case Type** (schema).

---

## Phase 2 — Design test cases

For each case, build an internal row matching [test-case-xlsx-schema.md](test-case-xlsx-schema.md):

- Assign **TC_ID** (`TC_<MOD>_<CAT>_<NNN>`).
- Set **Test Case Type** to exactly one allowed value.
- **Automation (Y/N):** default `N` unless user marks automation candidates.
- Leave execution/remark columns blank.
- **JIRA User Story ID** = `STORY_KEY`.
- **JIRA Test Case ID** = blank until Phase 4.

**Quality gate**

- Every AC maps to ≥1 test case.
- At least one **Functional Testing** case per story.
- Include **Regression Testing** when scope is `regression` or `full`.
- Add Integration / Security / etc. only when requirements or architecture justify them.
- One **overall Expected Result** per case; steps numbered in **Test Steps**.

---

## Phase 3 — Duplicate check

JQL:

```text
parent = <STORY_KEY> AND issuetype in subTaskIssueTypes() ORDER BY created DESC
```

Skip creating a sub-task if summary contains the same **TC_ID** or label `manual-test` + same TC_ID. Report skipped keys.

---

## Phase 4 — Create Jira Sub-tasks

For each new case (sequential, rate-limit friendly):

1. `getJiraProjectIssueTypesMetadata` / `getJiraIssueTypeMetaWithFields` for Sub-task required fields.
2. `createJiraIssue`:
   - `projectKey`: `CCIPH`
   - `issueTypeName`: Sub-task type from metadata
   - `parent`: `STORY_KEY`
   - `summary`: `[TC_ID] Title`
   - `description`: [jira-subtask-template.md](jira-subtask-template.md)
   - `additional_fields`: priority, labels (`manual-test`, `TC_ID`, `tc-type-*`), component **Customs Charges UI**
3. Capture returned key → set **JIRA Test Case ID** on the row.

Verify each sub-task appears under the story (`getJiraIssue` on parent or child).

---

## Phase 5 — Export xlsx (+ CSV)

1. Write CSV to:

   `qa/manual-test-cases/exports/xlsx/<ScreenSlug>_<STORY_KEY>_<YYYY-MM-DD>.csv`

   Headers **must** match the template (see schema).

2. Copy template:

   `.cursor/cci-skills/Sample File Reference/Test Case.xlsx`

   to:

   `qa/manual-test-cases/exports/xlsx/<ScreenSlug>_<STORY_KEY>_<YYYY-MM-DD>.xlsx`

3. Run:

   ```powershell
   .\qa\manual-test-cases\scripts\export-test-case-xlsx.ps1 `
     -CsvPath "qa/manual-test-cases/exports/xlsx/<file>.csv" `
     -OutputXlsx "qa/manual-test-cases/exports/xlsx/<file>.xlsx"
   ```

4. If Excel COM fails: deliver CSV + template path; user pastes into xlsx.

Optional: copy the same xlsx into `qa/manual-test-cases/templates/` only when updating the canonical template (do not overwrite Sample File Reference without user request).

---

## Phase 6 — Deliver

```markdown
## CCI Manual Test Cases — Sub-tasks + XLSX

**Story:** CCIPH-### — <title>
**Screen:** <Screen Name>
**Scope:** regression
**XLSX:** qa/manual-test-cases/exports/xlsx/...
**CSV:** qa/manual-test-cases/exports/xlsx/...

### Coverage by Test Case Type
| Test Case Type | Count |
|----------------|-------|
| Functional Testing | n |
| Integration Testing | n |
| ... | |

### Sub-tasks
| TC_ID | Test Case Type | JIRA Test Case ID | Summary |
|-------|----------------|-------------------|---------|
| TC_GS_FUNC_001 | Functional Testing | CCIPH-#### | ... |

### AC mapping
- AC1: TC_...

### Gaps / assumptions
- ...

### Next step (automation)

```
/cci-automation-from-story CCIPH-###
```

Same story key; discovers these sub-tasks (or linked Test Cases from `qa-manual-test-case`). Uses `.cursor/cci-skills/FE-ApplicationCode` and `BE-ApplicationCode` for locators.
```

---

## Error handling

| Situation | Action |
|-----------|--------|
| MCP not authenticated | Stop; connect Atlassian MCP |
| Sub-task type missing | List issue types; ask once |
| Parent cannot have sub-tasks | Stop; user must use Story |
| Partial Jira failure | Report created vs failed; still export xlsx for all designed rows (mark missing JIRA IDs) |
| draft only | Skip Phase 4; export with empty JIRA Test Case ID column |

## Do not

- Create **Test Case** issue type + `createIssueLink` (that is `qa-manual-test-case`, not this skill)
- Combine multiple TC_IDs in one sub-task
- Invent screens not in Jira/Figma/architecture/FE routes
- Use free-text Test Case Type — only schema allowed values
- Skip xlsx export when Jira succeeds

## MCP quick reference

| Step | Tool |
|------|------|
| Story / children | `getJiraIssue`, `searchJiraIssuesUsingJql` |
| Confluence | `getConfluencePage`, `searchConfluence` |
| Create sub-task | `createJiraIssue` (with `parent`) |
| Metadata | `getJiraProjectIssueTypesMetadata`, `getJiraIssueTypeMetaWithFields` |
