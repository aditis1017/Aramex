---
name: cci-e2e-qa-pipeline
description: >-
  End-to-end CCIPH QA pipeline from a User Story: functional test cases as Jira
  Sub-tasks + Test Case.xlsx, Katalon automation (required TCs only) using
  cci-skills FE/BE, Jira label Automated and status QA in-Progress, assignee =
  triggering user, then CCI-RTM.xlsx traceability. Invoke with
  /cci-e2e-qa-pipeline CCIPH-### or full e2e / e2e-pipeline wording — not bare
  story key alone.
---

# CCI End-to-End QA Pipeline

Orchestrates **functional manual TCs → selective automation → RTM** for one **CCIPH User Story**.

| Phase | Output |
|-------|--------|
| 1 | Functional sub-tasks + **Test Case.xlsx** |
| 2 | Katalon OR / Keywords / Scripts / Suite (Automation **Y** only) |
| 3 | Jira **`Automated`** label + **`QA in-Progress`** + assignee = trigger user |
| 4 | **CCI-RTM.xlsx** |

Config: [pipeline-config.md](pipeline-config.md) · Environments: [environments.md](environments.md) · RTM: [rtm-schema.md](rtm-schema.md) · Jira: [jira-workflow.md](jira-workflow.md)

## Do not confuse issue types

| Issue | Role in this pipeline |
|-------|----------------------|
| **User Story** | Input `STORY_KEY` |
| **Dev sub-tasks** (Backend/Frontend labels) | **Requirements only** — not automated |
| **QA sub-tasks** (`manual-test`, `Functional-Test-Case`) | **Functional test cases** — automated when `Automation (Y/N)=Y` |

This pipeline does **not** use `qa-manual-test-case` (linked **Test Case** issue type). For that path use `/qa-manual-test-case` then `/co-pilot-automation-skill`.

## Invocation

| Message | Action |
|---------|--------|
| `/cci-e2e-qa-pipeline CCIPH-385` | Full pipeline |
| `Full e2e QA pipeline for CCIPH-385` | Same |
| `/e2e-pipeline CCIPH-385` | Same |

**Not** triggered by bare `CCIPH-385` without e2e/pipeline wording.

Optional: `scope smoke|regression|full`, `automate all`, `draft only` (Phase 1 only, no Jira/automation), **`environment dev`** (default) or **`environment staging`**.

## Prerequisites

- Atlassian MCP **`user-atlassian`**
- [cci-testcase.mdc](../../rules/cci-testcase.mdc) coverage mindset
- Sub-skills: [cci-manual-tc-subtask-xlsx](../../cci-skills/cci-manual-tc-subtask-xlsx/SKILL.md), [cci-automation-from-story](../cci-automation-from-story/SKILL.md)

---

## Phase 0 — Story, people, dev context

1. Validate `STORY_KEY` = `CCIPH-\d+`.
2. `getAccessibleAtlassianResources` → `cloudId`.
3. **`atlassianUserInfo`** → store **`TRIGGER_ASSIGNEE_ID`** and display name.
4. `getJiraIssue(STORY_KEY)` — full story + epic link if present.
5. JQL dev/requirement sub-tasks: `parent = STORY_KEY AND issuetype in subTaskIssueTypes()` — classify by **Backend** / **Frontend** labels (see [qa-jira-defect-logger-subtask-assignee](../qa-jira-defect-logger-subtask-assignee/assignee-rules.md) label table). **Do not** treat these as test cases.
6. Load attachments, Confluence links, `.cursor/cci-skills/Required Files/`, [ui-product-context.md](../qa-manual-test-case/ui-product-context.md), `qa/traceability/module-map.yaml`.

Derive **MODULE_ROOT**, **MODULE_ABBR**, **EPIC_KEY** (if any).

### Step 0B — Resolve test environment (mandatory)

Follow [environments.md](environments.md):

1. Parse chat for `environment dev` | `environment staging` | `env staging` | `on staging` (default **`dev`**).
2. Set **`BASE_URL`** and **`MODULE_URL`** for `MODULE_ROOT` from the environment table.
3. Include **Environment** and **Application URL** in every manual TC precondition (Phase 1) and in the Phase 5 delivery report.

---

## Phase 1 — Functional test cases (xlsx + QA sub-tasks)

Execute [cci-manual-tc-subtask-xlsx](../../cci-skills/cci-manual-tc-subtask-xlsx/SKILL.md) logic with these **pipeline overrides**:

| Override | Value |
|----------|--------|
| Assignee | **`TRIGGER_ASSIGNEE_ID`** on every new QA sub-task |
| Extra labels | `Functional-Test-Case`, `e2e-pipeline` |
| Automation (Y/N) | Per [pipeline-config.md](pipeline-config.md) defaults; user `automate all` → all `Y` |
| Export | `qa/manual-test-cases/exports/xlsx/<Slug>_<STORY>_<date>.csv` + xlsx via `export-test-case-xlsx.ps1` |

After Phase 1, retain in memory for each row:

- `TC_ID`, sub-task **key**, title, Test Case Type, **Automation (Y/N)**

**draft only:** stop after xlsx/CSV plan or local export without Jira create.

---

## Phase 2 — Automation (required test cases only)

Execute [cci-automation-from-story](../cci-automation-from-story/SKILL.md) with:

- `subtasks-only`
- `required` — only sub-tasks where xlsx row has **Automation (Y/N) = Y**
- **`environment <dev|staging>`** — same as Step 0B (or `url <MODULE_URL>`)
- Mandatory [fe-be-locator-playbook.md](../cci-automation-from-story/fe-be-locator-playbook.md): grep `.cursor/cci-skills/FE-ApplicationCode` and `BE-ApplicationCode/services/`
- Reuse repo `Object Repository/` and `Keywords/` before creating new assets
- [framework-structure.md](../cci-automation-from-story/framework-structure.md)
- Suite: `Test Suites/CCI_<MODULE_ABBR>_<STORY>_SUITE.ts`
- `no jira update` for automation skill’s default labels — Phase 3 applies **`Automated`** instead

If zero `Y` rows: skip Phase 2; report; still run Phase 4 RTM with `Not Applicable` automation status.

---

## Phase 3 — Jira post-automation (per automated QA sub-task)

For each sub-task that received Katalon `.tc` + script:

1. **`editJiraIssue`** — merge labels: **`Automated`**, `katalon` (optional)
2. **Transition** to **`QA in-Progress`** — [jira-workflow.md](jira-workflow.md)
3. **Comment** with Katalon paths
4. Update Test Case.xlsx CSV: `Automation Execution Status` = `Not Run`, `Automation Remarks` = script file name; re-run `export-test-case-xlsx.ps1` if needed

Non-automated sub-tasks: no `Automated` label; status unchanged.

Assignee remains **trigger user** unless Jira workflow reassigns on transition.

---

## Phase 4 — Traceability matrix (CCI-RTM)

For **every** QA sub-task from Phase 1, one RTM row per [rtm-schema.md](rtm-schema.md):

| Column | Value |
|--------|--------|
| JIRA Epic ID | `EPIC_KEY` or empty |
| JIRA User Story ID | `STORY_KEY` |
| JIRA User Story Title | story summary |
| JIRA Test Case ID | sub-task key |
| JIRA Test Case Title | title |
| Automation Status | `Automated` if scripted; else `Not Applicable` |
| Remark | script path or manual-only note |

1. Write CSV: `qa/traceability/rtm/CCI-RTM_<STORY>_<date>.csv`
2. Run:

```powershell
.\qa\manual-test-cases\scripts\export-cci-rtm-xlsx.ps1 `
  -CsvPath "qa/traceability/rtm/CCI-RTM_<STORY>_<date>.csv" `
  -OutputXlsx "qa/traceability/rtm/CCI-RTM_<STORY>_<date>.xlsx"
```

---

## Phase 5 — Delivery

```markdown
## CCI E2E QA Pipeline — Complete

**Story:** CCIPH-### — <title>
**Triggered by:** <display name> (assignee on QA sub-tasks)
**Environment:** dev | staging — **BASE_URL:** …
**Epic:** <key or N/A>

### Phase 1 — Functional
- Sub-tasks created: N
- Test Case xlsx: <path>

### Phase 2 — Automation
- Automated: M (Automation Y)
- Skipped manual-only: N-M
- Suite: Test Suites/...
- FE/BE files referenced: <list>

### Phase 3 — Jira updates
| Sub-task | Automated label | QA in-Progress | Notes |
|----------|-----------------|----------------|-------|

### Phase 4 — RTM
- <path to CCI-RTM xlsx>

### Next steps
- Run suite in Katalon Studio
- `qa-jira-suite-result-updater` with same story key
- Defects: `qa-jira-defect-logger-subtask-assignee`
```

---

## Error handling

| Situation | Action |
|-----------|--------|
| MCP unavailable | Stop Phase 0 |
| No sub-tasks after Phase 1 failure | Stop; do not run automation |
| Transition fails | Comment + report; continue RTM |
| Excel COM fails | Deliver CSV + template paths |
| Partial automation | RTM + report list successes/failures |

## Do not

- Run on bare story key without e2e/pipeline intent
- Automate dev Backend/Frontend sub-tasks
- Use `qa-manual-test-case` Test Case + link model in this pipeline
- Skip RTM after successful Phase 1
- Assign QA sub-tasks to dev sub-task reporters (use **trigger user** only)

## MCP tools

| Step | Tools |
|------|--------|
| Story / sub-tasks | `getJiraIssue`, `searchJiraIssuesUsingJql` |
| Trigger user | `atlassianUserInfo` |
| Create sub-tasks | `createJiraIssue` (parent), `editJiraIssue` (assignee) |
| Post-auto | `editJiraIssue`, `transitionJiraIssue` (or equivalent), `addOrEditJiraIssueComment` |
