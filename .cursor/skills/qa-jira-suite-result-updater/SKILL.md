---
name: qa-jira-suite-result-updater
description: >-
  Katalon suite → Jira sync from Report + User Story. Parse the Test Suite Report
  and the Jira User Story (linked Test Cases), update each matching Jira TC as
  PASS or FAIL, and auto-create Bugs for corresponding failed TCs (duplicate-checked,
  linked to TC + story). Trigger on /qa-jira-suite-result-updater with a Reports/
  path and story key (e.g. CCIPH-385), or sync suite results for a story.
  Always auto-apply without asking Yes/Proceed.
---

# QA Jira Suite Result Updater

You are a senior QA engineer. After a **Katalon Test Suite** run, sync results to Jira against a **User Story**.

**Primary contract:** The user passes **two inputs**:

1. **Test Suite Report** (`Reports/...` folder or `JUnit_Report.xml`)
2. **Jira User Story** key (e.g. `CCIPH-385`)

You then:

1. **Parse** the suite **Test Report** (`JUnit_Report.xml` preferred).
2. **Parse / fetch** the **Jira User Story** and its **linked Test Cases**.
3. **Map** each Katalon result to the corresponding Jira Test Case under that story.
4. **Update** each matched Jira TC as **PASS** or **FAIL** (comment + labels + status when possible).
5. For every **FAIL**, **create / log a Bug** for that failed TC (duplicate-checked) and **link** Bug → Test Case **and** Bug → User Story.

Reuse defect content rules from [../qa-jira-defect-logger/SKILL.md](../qa-jira-defect-logger/SKILL.md) and [../qa-jira-defect-logger/defect-template.md](../qa-jira-defect-logger/defect-template.md). Do **not** skip duplicate checks before creating bugs.

**Do not hardcode** a Jira project, board, or site. Derive project from the story key prefix (Phase 0).

---

## Invocation (minimal chat — auto-run)

**Required inputs:**

| Input | Example |
|-------|---------|
| Test Suite Report | `Reports/20260728_152604/CCI_APM_CCIPH-385_SUITE/20260728_152604` |
| Jira User Story | `CCIPH-385` |

When **both** appear in chat (or with this skill attached), **immediately** run end-to-end:

**parse report → fetch story + linked TCs → map → update Jira PASS/FAIL → log Bug per FAIL → deliver summary**

Do **not** wait for project key, mode, or “Yes / Proceed / Continue”.  
Do **not** ask for confirmation before writing to Jira.

If only one of the two is given:

- Report only → try to resolve story from suite folder name / script headers; if still missing, **ask for the story key once**.
- Story only → use latest matching suite report under `Reports/` that embeds the story key; if none, **ask for the report path once**.

### Accepted triggers (all equivalent)

| User message | Action |
|--------------|--------|
| `Reports/.../20260728_152604` + `CCIPH-385` | Full sync |
| `/qa-jira-suite-result-updater Reports/... CCIPH-385` | Full sync |
| `Sync suite report for story CCIPH-385: Reports/...` | Full sync |
| `Update TCs and log bugs for CCIPH-385 from Reports/...` | Full sync |
| Story key + “latest report” | Resolve latest report for that story, then full sync |

Optional overrides (never required): `UPDATE_ONLY`, `DEFECTS_ONLY`, explicit `Katalon name → Jira key` map.

### Default mode

| Setting | Default |
|---------|---------|
| Mode | `UPDATE_AND_DEFECTS` |
| Create defects for failures | `true` |
| Auto-apply without confirmation | **`true`** (always) |

Show a short progress note while running if useful; **do not** pause at a confirmation matrix.

---

## Inputs

| Parameter | Required | Default |
|-----------|----------|---------|
| **Katalon Test Suite Report** (path or folder) | **Yes** | “latest” for the given story if user says so |
| **Jira User Story ID** (e.g. `PROJ-123`) | **Yes** | From suite name / scripts only as fallback |
| Jira project key | No | Prefix of story key (e.g. `CCIPH-385` → `CCIPH`) |
| Jira site / browse base URL | No | MCP resources or [doc-index.md](../qa-knowledge-retriever/doc-index.md) |
| Mode | No | `UPDATE_AND_DEFECTS` |
| Explicit TC map | No | — |

### Modes

| Mode | Behaviour |
|------|-----------|
| `UPDATE_ONLY` | PASS/FAIL comments (+ transitions/labels). No bugs. |
| `DEFECTS_ONLY` | Parse failures only; log bugs. No PASS updates. |
| `UPDATE_AND_DEFECTS` | Full sync (**default**). |

### Project resolution (mandatory — no hardcoding)

Resolve `<PROJECT_KEY>` in this order; stop at the first usable value:

1. User input (if any)
2. **Prefix of the User Story ID** (e.g. `ABC-100` → `ABC`) — preferred when story is provided
3. Prefix of any mapped Jira Test Case key
4. Suite folder pattern embedding a key
5. **[doc-index.md](../qa-knowledge-retriever/doc-index.md)** → `Jira.Project Key`
6. **Ask the user** — only if still unresolved

Resolve **browse base URL** as:

1. User-provided site URL
2. `getAccessibleAtlassianResources` → site URL
3. doc-index → `Atlassian Site.Site URL`
4. Fallback: `https://<site-from-MCP>/browse/<KEY>`

Store for the run: `PROJECT_KEY`, `SITE_URL`, **`STORY_KEY`** (required), `SUITE_NAME`, `REPORT_PATH`.

---

## Phase 0 — Prerequisites

1. Capture `REPORT_PATH` and `STORY_KEY` from the user message (or resolve fallbacks above).
2. Resolve `SITE_URL` / `cloudId` via `getAccessibleAtlassianResources` if unknown.
3. Prefer MCP server **plugin-atlassian-atlassian** or **user-atlassian**.
4. If MCP auth fails: stop and tell the user to connect Atlassian MCP in Cursor Settings.
5. Set `PROJECT_KEY` from the story key prefix; optionally confirm via `getVisibleJiraProjects`.
6. `getJiraIssue` on `STORY_KEY` — must succeed before mapping. If the story is missing or inaccessible, stop and tell the user.

---

## Phase 1 — Parse the Test Suite Report

### Step 1a — Find the report

If the user gave a path:

- Folder → use `JUnit_Report.xml` inside it (or nested timestamp folder).
- File → use that file’s parent folder as the report root.
- HTML report → prefer sibling `JUnit_Report.xml`; else parse HTML as fallback.

If the user said “latest” for a known story: prefer the newest `Reports/` folder whose path or suite name contains `STORY_KEY`.

```
Reports/<timestamp>/<suite-path>/<timestamp>/
```

Priority of files:

1. `JUnit_Report.xml`
2. HTML report in the same folder
3. `execution0.log` / `console0.log` (supplementary only)

If multiple equally plausible candidates: list the latest **3** and ask which one — **only** in that case.

### Step 1b — Extract every testcase result

From `JUnit_Report.xml`, read **all** `<testcase>` entries (not only failures).

| Result | Rule |
|--------|------|
| **FAIL** | Has `<failure>` or `status="FAILED"` / `ERROR` treated as fail for Jira |
| **PASS** | No failure/error; `status="PASSED"` or successful completion |
| **SKIP** | `status="SKIPPED"` / `<skipped>` — comment only; do not create defects |

Capture per case:

| Field | Source |
|-------|--------|
| Katalon TC name | `testcase/@name` (leaf name) |
| Suite name | `testsuite/@name` |
| Status | PASS / FAIL / SKIP |
| Failure message | `failure/@message` (FAIL only) |
| Stack / logs | `failure` body, `system-out` |
| Duration | `testcase/@time` |
| Timestamp | `testsuite/@timestamp` |
| Environment | `properties`: browser, os, katalonVersion, hostName |
| Screenshots | `attachments` / `[[ATTACHMENT|...]]` |
| Report path | folder containing `JUnit_Report.xml` |

**Redact** passwords, tokens, OTP, cookies from anything written to Jira.

### Step 1c — Build the Result Matrix (internal)

```markdown
## Suite Result Matrix

| Katalon TC | Status | Duration | Jira TC (TBD) | Story |
|------------|--------|----------|---------------|-------|
| <TC_NAME_1> | FAIL | 42s | | <STORY_KEY> |
| <TC_NAME_2> | PASS | 31s | | <STORY_KEY> |
```

Proceed immediately to Phase 1.5–5. Do not ask for confirmation.

---

## Phase 1.5 — Parse the Jira User Story (mandatory)

Fetch and parse `STORY_KEY` before mapping:

1. `getJiraIssue` on `STORY_KEY` — capture summary, status, description, issue links.
2. Collect **all linked Test Case candidates** from the story:
   - Link types: `Test`, `Relates`, `Relates to`, `Tests`, `is tested by`, `is verified by`
   - Keep issues whose type is `Test Case` / `Test` / `Xray Test`, **or** summary starts with `TC_`, **or** labels include `ui-test` / `api-test` / `automation`, **or** description contains `Test Case Details`
3. Optionally `searchJiraIssuesUsingJql`:
   ```
   project = <PROJECT_KEY> AND issue in linkedIssues(<STORY_KEY>) AND (summary ~ "TC_" OR labels = automation)
   ```
4. Build an internal **Story TC inventory**:

```markdown
## Story TC inventory — <STORY_KEY>

| Jira TC | Summary | Status |
|---------|---------|--------|
| <TC_KEY_1> | TC_APM_001 … | … |
```

This inventory is the **preferred mapping source** for Katalon results under this story.

---

## Phase 2 — Map Katalon TC → Jira Test Case (story-scoped)

Resolve **one Jira issue key per Katalon test**. Prefer TCs linked to **`STORY_KEY`**.

Search order:

1. **Story-linked inventory** (Phase 1.5) — match by:
   - Katalon leaf name contained in Jira summary / description
   - `TC_…` id shared between Katalon name and Jira summary
2. **Script header** in `Scripts/**/<KatalonTCName>/Script*.groovy`:
   - Prefer: `// Jira: <TC_KEY> | Story: <STORY_KEY>`
   - Accept only if `Story:` matches `STORY_KEY` **or** the Jira TC is linked to `STORY_KEY`
3. **Test Case `.tc` description / comment** under `Test Cases/`
4. **User-provided map** (if any)
5. **JQL** (fallback, still prefer story linkage):
   ```
   project = <PROJECT_KEY> AND (summary ~ "<TC_ID>" OR summary ~ "<Katalon leaf name>" OR description ~ "<TC_ID>")
   ORDER BY updated DESC
   ```
   Prefer a hit that is linked to `STORY_KEY`.

### Generic test-case candidate rules

Treat an issue as a test-case candidate if **any** of the following is true:

- Issue type is `Test Case`, `Test`, or `Xray Test`
- Issue is linked to `STORY_KEY` **and** (`summary` starts with `TC_` **or** labels include `ui-test` / `api-test` / `automation` **or** description contains `Test Case Details`)

If a Katalon TC cannot be mapped: mark it **UNMAPPED** — report in delivery; **do not** invent a Jira key; **do not** create a bug without a target test-case key.

Set **Story** on every mapped row to `STORY_KEY`.

If mapped keys belong to a different project than the story’s project: skip those keys and note them unless the user allowed multi-project sync.

---

## Phase 3 — Auto-apply (no confirmation gate)

After mapping:

1. Immediately run Phase 4 for all mapped rows.
2. Immediately run Phase 5 for all FAIL rows (mode includes defects).
3. Deliver Phase 6 summary.

Optional: one-line status (“Syncing N test cases for `<STORY_KEY>`…”) — never block on user input.

Exception — ask the user **only** when blocked:

- Missing report or story (after fallbacks)
- Ambiguous latest report (list 3)
- Story key invalid / inaccessible
- Project key still unresolved
- Bug create fails on required custom fields with no safe default
- MCP auth failure

---

## Phase 4 — Update Jira Test Cases (PASS / FAIL)

For each **mapped** row (skip SKIP for transitions; still add a brief SKIP comment if useful):

### Step 4a — Execution result comment (mandatory)

Use [result-comment-template.md](result-comment-template.md). Substitute `<SITE_URL>` for browse links.

Post via `addCommentToJiraIssue` on the **Jira Test Case** key (`contentFormat: markdown`).

### Step 4b — Labels (recommended)

Via `editJiraIssue` when safe:

| Result | Labels to set / refresh |
|--------|-------------------------|
| PASS | Add `automation-pass`; remove `automation-fail` if present |
| FAIL | Add `automation-fail`; remove `automation-pass` if present |

Also keep/add: `automation`, `katalon`.

If label edit fails: continue (comment is enough); note in delivery summary.

### Step 4c — Status transition (best effort)

1. `getTransitionsForJiraIssue` for the Test Case.
2. Prefer transition names matching:

| Result | Preferred transition names (case-insensitive contains) |
|--------|--------------------------------------------------------|
| PASS | `Pass`, `Passed`, `Done`, `Resolved`, `Verified` |
| FAIL | `Fail`, `Failed`, `Blocked`, `In Progress`, `Reopened` |

3. `transitionJiraIssue` with the chosen transition `id`.

If no suitable transition exists: **do not fail the skill** — comment + labels are the source of truth for PASS/FAIL.

### Step 4d — Parent story summary (once per suite if any FAIL)

After Phase 5, add **one** summary comment on `STORY_KEY` listing failed Jira TC keys and bug keys. Do not spam the story per TC.

---

## Phase 5 — Log Bug for each corresponding failed TC (auto)

For each **FAIL** with a mapped Jira Test Case:

1. Follow defect-logger content rules (reuse the failure bundle). **Override** “wait for confirmation”: decide automatically per rules below.
2. Create bugs in the same `PROJECT_KEY` as the story / Test Case.
3. **Duplicate check** (mandatory) with JQL including `PROJECT_KEY`, the **Jira TC key**, Katalon TC id, and preferably `STORY_KEY`.

### Auto duplicate decision (no user wait)

| Match confidence | Action |
|------------------|--------|
| ≥ 90% (same TC + same root cause, **open** bug) | Comment on existing bug with this suite’s evidence; do **not** create a duplicate |
| &lt; 90% or no match | **Create** new Bug using [../qa-jira-defect-logger/defect-template.md](../qa-jira-defect-logger/defect-template.md) — set **Related Story** to `STORY_KEY` |

4. **Linking (mandatory for new bugs):**

| Link | Type | Purpose |
|------|------|---------|
| Bug ↔ **Jira Test Case** | `Relates` (or `Tests` / `is tested by` if available) | Defect ↔ failed TC |
| Bug ↔ **User Story** (`STORY_KEY`) | `Relates` | Defect ↔ requirement — **always** when story is known |

Use `createIssueLink`. Link Test Case first, then Story.

5. On the **Jira Test Case**, add a short comment pointing to the Bug key (if not already in the FAIL comment).

**Do not** create a defect for PASS or SKIP.  
**Do not** create one bug per identical root cause without consolidating — if several TCs fail for the **same** clear root cause in one suite, create **one** bug listing all TC keys, linked to each Jira TC **and** to `STORY_KEY`.

---

## Phase 6 — Deliver

```markdown
## Suite → Jira sync complete

**Project:** <PROJECT_KEY>
**Story:** <STORY_KEY>
**Suite:** <SUITE_NAME>
**Report:** `Reports/.../JUnit_Report.xml`
**Totals:** Pass <n> | Fail <n> | Skip <n> | Unmapped <n>

### Jira Test Case updates
| Jira TC | Katalon TC | Result | Comment | Transition | Labels |
|---------|------------|--------|---------|------------|--------|
| <TC_KEY_1> | <TC_NAME_1> | FAIL | Added | Failed (id) / skipped | automation-fail |
| <TC_KEY_2> | <TC_NAME_2> | PASS | Added | Done (id) / skipped | automation-pass |

### Defects (failed TCs only)
| Bug | Action | Linked Test Case | Story | Root cause |
|-----|--------|------------------|-------|------------|
| <BUG_KEY_1> | Created | <TC_KEY_1> | <STORY_KEY> | ... |
| <BUG_KEY_2> | Comment only (duplicate) | <TC_KEY_3> | <STORY_KEY> | ... |

### Unmapped / skipped
- ...
```

Include browse URLs: `<SITE_URL>/browse/<KEY>`.

---

## Error handling

| Situation | Action |
|-----------|--------|
| Story key missing / invalid | Ask once for story; do not guess |
| Project key unresolved | Ask user; do not guess |
| No report found | List latest 3 under `Reports/`; ask user |
| Empty suite / zero testcases | Stop; say report has no tests |
| Atlassian MCP unauthenticated | Stop; ask user to connect MCP |
| Unmapped Katalon TC | List in delivery; continue others |
| Transition not available | Comment + labels only; note in summary |
| Bug type missing | Use `getJiraProjectIssueTypesMetadata`; pick closest; ask if unclear |
| Required Bug fields missing | `getJiraIssueTypeMetaWithFields`; ask user only if blocked |
| Partial MCP failure mid-run | Continue remaining TCs; report successes and failures separately |

## Do not

- Hardcode a Jira project, board, or site URL
- Invent Jira keys or mark PASS/FAIL without report evidence
- Create duplicate bugs without JQL duplicate check
- Put credentials, tokens, or full raw logs in Jira
- Update issues outside the story’s **resolved** `PROJECT_KEY` unless the user allows multi-project sync
- Skip linking Bug → failing **Test Case** or Bug → **User Story** when keys are known
- Wait for Yes/Proceed when report + story were provided
- Create bugs for PASS / SKIP results
- Treat this skill as a substitute for Xray/Zephyr native test executions unless the user only needs comment/status sync

## Quick reference — MCP tools

| Step | Tool |
|------|------|
| Cloud ID / site | `getAccessibleAtlassianResources` |
| Confirm project | `getVisibleJiraProjects` |
| Fetch story + links | `getJiraIssue` |
| Map / find TC | `searchJiraIssuesUsingJql`, `getJiraIssue` |
| PASS/FAIL comment | `addCommentToJiraIssue` |
| Labels | `editJiraIssue` |
| Status | `getTransitionsForJiraIssue`, `transitionJiraIssue` |
| Create bug | `createJiraIssue` |
| Link bug ↔ TC / story | `createIssueLink` |
| Duplicate search | `searchJiraIssuesUsingJql` |

## Related skills

| Skill | When |
|-------|------|
| [../qa-jira-defect-logger/SKILL.md](../qa-jira-defect-logger/SKILL.md) | Single failure / ad-hoc bug only |
| [../qa-knowledge-retriever/doc-index.md](../qa-knowledge-retriever/doc-index.md) | Default site / project mapping (optional) |
| [../co-pilot-skill/SKILL.md](../co-pilot-skill/SKILL.md) | Creating Jira Test Cases (product-specific flows) |
| [../co-pilot-automation-skill/SKILL.md](../co-pilot-automation-skill/SKILL.md) | Generating Katalon scripts with `// Jira: …` headers |
