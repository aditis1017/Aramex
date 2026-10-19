---
name: qa-suite-failed-bug-done-retry
description: >-
  From a Katalon Test Suite report, build and optionally run a retry suite containing
  only failed test cases that are linked to Jira Bugs whose status is Done (not New).
  Use when the user asks to re-run failed TCs for fixed bugs, bug-done verification,
  or invokes /qa-suite-failed-bug-done-retry with a suite path, report path, or Bug key.
---

# QA Suite Failed — Bug Done Retry

You are a senior QA automation engineer. After a **Test Suite** run, create (and optionally **trigger**) a Katalon suite that includes **only** test cases that **failed** in that run **and** are traceable to at least one **eligible** Jira **Bug**.

Reuse report parsing from [../qa-jira-defect-logger/SKILL.md](../qa-jira-defect-logger/SKILL.md) Phase 1 and Jira TC mapping from [../qa-jira-suite-result-updater/SKILL.md](../qa-jira-suite-result-updater/SKILL.md) Phase 2. Reuse suite XML rules from [retry-suite-template.md](retry-suite-template.md).

## Bug eligibility (mandatory)

Apply this filter to every linked Bug candidate. Use the Jira issue **status name** (`fields.status.name`), case-insensitive.

**Bugs ticket != New && Bug Ticket == Done**

| Rule | Implementation |
|------|----------------|
| Not New | `status.name` is not `New` |
| Done | `status.name` is `Done` |

A failed Katalon TC is **included** only if it maps to a Jira Test Case (or explicit Bug link below) with **at least one** linked Bug that passes **both** rules.

If the user passes an explicit `BUG-KEY` / `CCIPH-###` Bug key, still validate eligibility; if the Bug is not Done, **exclude** its TCs and report why.

## Trigger examples

- `/qa-suite-failed-bug-done-retry Test Suites/CCI_APM_CCIPH-634_SUITE` + latest report
- `Reports/20260728_152604/CCI_APM_CCIPH-385_SUITE/20260728_152604` + `CCIPH-385`
- `Re-run failed TCs linked to CCIPH-643 where bug is Done`
- `Create bug-done retry suite and run in CI` (after suite is written and pushed if needed)

## Inputs

| Parameter | Required | Default |
|-----------|----------|---------|
| **Source Test Suite** (`Test Suites/<name>` without `.ts`) or suite name from report | Yes* | From report `testsuite/@name` if report given |
| **Katalon report** (`Reports/...` or `JUnit_Report.xml`) | Yes* | Latest under `Reports/` matching source suite folder name |
| Jira User Story (e.g. `CCIPH-385`) | No | From suite name / tags / script headers |
| Explicit Bug key(s) | No | Discover via links from Jira TC |
| `run` | No | `no` — create suite only |
| `environment` / execution profile | No | `dev` (match [aramex-ci.yml](../../.github/workflows/aramex-ci.yml) inputs) |
| `push` + CI | No | Only if user asks — follow [../github-aramex-push/SKILL.md](../github-aramex-push/SKILL.md) |

\*Provide **report**, **source suite**, or **both**. If only one is given, resolve the other using fallbacks in Phase 1.

## Phase 0 — Prerequisites

1. Resolve `cloudId` via `getAccessibleAtlassianResources` if unknown.
2. Prefer MCP **plugin-atlassian-atlassian** or **user-atlassian**.
3. Resolve `PROJECT_KEY` from story prefix, Bug prefix, or [../qa-knowledge-retriever/doc-index.md](../qa-knowledge-retriever/doc-index.md).
4. If MCP auth fails: stop and instruct the user to connect Atlassian MCP.

## Phase 1 — Parse failures from the Test Suite report

### Find the report

Same rules as qa-jira-suite-result-updater Phase 1a:

```
Reports/<timestamp>/<suite-path>/<timestamp>/JUnit_Report.xml
```

### Collect failed Katalon test cases

Every `<testcase>` with `status="FAILED"`, `ERROR`, or a `<failure>` child.

Build **Failed set** `F`: Katalon leaf names + `testCaseId` paths (`Test Cases/...`).

If `F` is empty: stop — nothing to retry; tell the user the report has no failures.

Record `SOURCE_SUITE` from `testsuite/@name` or the suite folder under `Reports/`.

## Phase 2 — Map failed Katalon TC → Jira Test Case

For each member of `F`, resolve one Jira Test Case key using the same order as qa-jira-suite-result-updater Phase 2 (story-linked inventory preferred when `STORY_KEY` is known).

Rows without a Jira TC key go to **Unmapped** — they are **not** included in the retry suite unless Phase 3 finds a direct Bug link (Phase 3b).

## Phase 3 — Resolve linked Bugs and apply Done filter

### Phase 3a — Bugs linked to Jira Test Case

For each mapped Jira Test Case `TC_KEY`:

1. `getJiraIssue` on `TC_KEY` — collect outward/inward links to issues of type **Bug** (also accept issue type name containing `Bug`).
2. Optional JQL:
   ```
   issue in linkedIssues(<TC_KEY>) AND type = Bug ORDER BY updated DESC
   ```

### Phase 3b — Direct Bug ↔ failure (fallback)

When mapping to Jira TC failed but the user named a Bug or labels/summary tie automation to a Bug:

```
project = <PROJECT_KEY> AND type = Bug AND (summary ~ "<Katalon leaf>" OR description ~ "<Katalon leaf>" OR labels = <TC_ID>)
```

Only use Bugs that also link to the same story or Test Case when story scope is required.

### Phase 3c — Eligibility

For each candidate Bug, read `fields.status.name`:

- **Eligible** iff status is **Done** and status is **not** **New**.
- Track `Bug → [eligible TC keys / Katalon IDs]`.

### Phase 3d — Build retry candidate set `R`

```
R = { katalon TC in F | ∃ eligible Bug linked to its Jira TC (or direct Phase 3b) }
```

Failed TCs with only **New**, **In Progress**, or other non-Done Bugs: list under **Excluded — bug not Done** in delivery.

Failed TCs with **no** Bug link: list under **Excluded — no linked bug**.

If `R` is empty: stop after delivery summary; do **not** write an empty suite.

## Phase 4 — Write Katalon retry Test Suite

Follow [retry-suite-template.md](retry-suite-template.md).

| Piece | Value |
|-------|--------|
| Name | `<SOURCE_SUITE>_BUG_DONE_RETRY_<yyyyMMdd>` |
| Tags | `failed-retry,bug-done,<STORY_KEY>,<SOURCE_SUITE>` |
| Cases | Only `R`, source suite order when `Test Suites/<SOURCE_SUITE>.ts` exists |
| Description | Report path, execution time, eligible Bug keys per TC |

Create `Test Suites/<name>.groovy` only if missing (stub from `Test Suites/CCI_APM_CCIPH-634_SUITE.groovy`).

## Phase 5 — Trigger execution (when `run` is requested)

Default: **create suite only** and give run instructions.

When the user sets `run: yes`, `run locally`, or `trigger CI`:

### Local (preferred on developer machine)

1. Open Katalon Studio → run `Test Suites/<name>`.
2. Or CLI when `KATALON_API_KEY` and `katalonc` are available (do not paste secrets into chat):
   - Project: `Corp.microservice.ccm-automation.prj`
   - `-testSuitePath="Test Suites/<name>"`
   - `-executionProfile="<profile>"`

### GitHub Actions

1. Ensure `Test Suites/<name>.ts` is committed; use [../github-aramex-push/SKILL.md](../github-aramex-push/SKILL.md) only if the user asked to push.
2. `gh workflow run aramex-ci.yml -f test_suite="Test Suites/<name>" -f environment=<profile>`
3. If the workflow **choice list** does not include the new suite, tell the user to add it under `workflow_dispatch.inputs.test_suite.options` in `.github/workflows/aramex-ci.yml`, or run locally until the list is updated.

Do **not** auto-push or auto-run CI unless the user explicitly requests it.

## Phase 6 — Deliver

```markdown
## Bug Done retry suite

**Source suite:** <SOURCE_SUITE>
**Report:** `Reports/.../JUnit_Report.xml`
**Story:** <STORY_KEY or n/a>
**Failed in report:** <n>
**Included in retry (Done bugs):** <m>
**Retry suite:** `Test Suites/<name>.ts` [Created | Updated]
**Run:** [Not requested | Local steps | CI workflow dispatched]

### Included (failed + eligible Bug)
| Katalon TC | Jira TC | Eligible bug(s) | Bug status |
|------------|---------|-----------------|------------|
| ... | ... | CCIPH-### | Done |

### Excluded
| Katalon TC | Reason |
|------------|--------|
| ... | No linked bug |
| ... | Bug CCIPH-### status=In Progress (requires Done) |
| ... | Bug CCIPH-### status=New |

### Unmapped
- ...

### Bug filter applied
Bugs ticket != New && Bug Ticket == Done
```

## Error handling

| Situation | Action |
|-----------|--------|
| No report / suite | List latest 3 reports; ask once |
| No failures | Stop; report clean suite |
| No eligible bugs | Stop; no suite file (or skip write) |
| MCP auth failure | Stop; connect Atlassian |
| TC path missing in repo | Drop from suite; note in delivery |
| User wants non-Done bugs | Explain filter; do not widen unless user explicitly overrides eligibility |

## Do not

- Include **passing** test cases in the retry suite
- Include failed cases **without** an eligible **Done** Bug (unless user explicitly overrides the filter)
- Include Bugs in **New** status
- Hardcode site URL or project — derive from keys and MCP
- Push or run CI without user request
- Fabricate Bug ↔ TC links

## Quick reference — MCP tools

| Step | Tool |
|------|------|
| Cloud ID | `getAccessibleAtlassianResources` |
| Story / TC / Bug | `getJiraIssue` |
| Linked bugs | `searchJiraIssuesUsingJql`, issue links from `getJiraIssue` |
| Transitions (optional comment on Bug) | `addOrEditJiraIssueComment` if user asks to note retry |

## Related skills

| Skill | When |
|-------|------|
| [../qa-jira-defect-logger/SKILL.md](../qa-jira-defect-logger/SKILL.md) | Log failures / all-failed retry suite (no Done filter) |
| [../qa-jira-suite-result-updater/SKILL.md](../qa-jira-suite-result-updater/SKILL.md) | Sync full suite results to Jira |
| [../github-aramex-push/SKILL.md](../github-aramex-push/SKILL.md) | Push suite then CI |
