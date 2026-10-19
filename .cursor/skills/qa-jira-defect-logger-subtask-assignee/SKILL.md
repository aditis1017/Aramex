---
name: qa-jira-defect-logger-subtask-assignee
description: >-
  Logs Jira defects from failed Katalon runs like qa-jira-defect-logger, but sets
  Bug assignee from the parent story's Sub-task (not the story reporter): Backend
  bugs → Backend-labeled sub-task reporter; Frontend/UI bugs → Frontend-labeled
  sub-task reporter. Use when filing automation bugs for CCIPH with dev ownership
  from sub-tasks, or when the user invokes this skill by name.
---

# QA Jira Defect Logger — Sub-task developer assignee (CCIPH)

You are a senior QA engineer. This skill extends [**qa-jira-defect-logger**](../qa-jira-defect-logger/SKILL.md) with **CCIPH-specific assignee rules** from **Sub-tasks** on the parent User Story.

**Do not** set Bug assignee from the User Story reporter.  
**Do** resolve assignee from the **Reporter** on the correct **Sub-task** (developer workstream), using **Backend** / **Frontend** labels — see [assignee-rules.md](assignee-rules.md).

Execute all phases from the parent skill **except** assignee handling, which is replaced by **Phase 2B** and **Phase 4B** below.

## When to use

| Use this skill | Use original qa-jira-defect-logger |
|----------------|-----------------------------------|
| CCIPH automation failure; dev assignee from Backend/FE sub-tasks | Generic bug logging without sub-task assignee rules |
| User says "assign bug to developer sub-task" | User explicitly wants story reporter |

## Trigger examples

- `/qa-jira-defect-logger-subtask-assignee TC_APM_FUNC_001 failed`
- `Log defect for CCIPH-102 TC_APM_FUNC_010 — frontend`
- `File automation bug from latest report, backend issue`
- `qa-jira-defect-logger-subtask-assignee Reports/...`

## Input you accept

All parameters from [qa-jira-defect-logger](../qa-jira-defect-logger/SKILL.md#input-you-accept), plus:

| Parameter | Required | Default |
|-----------|----------|---------|
| `layer` | No | Inferred — see [assignee-rules.md](assignee-rules.md) |
| `sub-task` / `from sub-task CCIPH-####` | No | Auto-pick by layer + labels |
| Jira project key | No | `CCIPH` from [doc-index.md](../qa-knowledge-retriever/doc-index.md) |

**Restrictions:** Only **CCIPH** parent stories and sub-tasks unless the user overrides project key.

---

## Phase 1 — Locate and parse the failure

**Identical** to [qa-jira-defect-logger Phase 1](../qa-jira-defect-logger/SKILL.md#phase-1--locate-and-parse-the-failure).

---

## Phase 2 — Resolve traceability

**Identical** to [qa-jira-defect-logger Phase 2](../qa-jira-defect-logger/SKILL.md#phase-2--resolve-traceability), with these additions:

1. Resolve `STORY_KEY` (`CCIPH-###`) before duplicate check or create.
2. Traceability fallbacks (in order):
   - User-provided story key
   - Katalon path `Test Cases/<Module>/<STORY_KEY>/...`
   - `qa/traceability/module-map.yaml` + TC name → module → recent story from suite name
   - [doc-index.md](../qa-knowledge-retriever/doc-index.md)
   - `searchJiraIssuesUsingJql` / Rovo search

If no story is found, ask for `STORY_KEY` before Phase 2B.

---

## Phase 2B — Resolve developer assignee (mandatory before create)

Follow [assignee-rules.md](assignee-rules.md) exactly.

### Step 2B.1 — Load sub-tasks

1. `getAccessibleAtlassianResources` → `cloudId` if needed.
2. Fetch all sub-tasks of `STORY_KEY` via JQL (`parent = STORY_KEY` + sub-task issue type).
3. Load each sub-task: `labels`, `summary`, `reporter`, `assignee`.

### Step 2B.2 — Determine defect layer

Apply priority table in assignee-rules (user override → TC labels → Katalon evidence → default **Frontend** for Web UI).

Record `DEFECT_LAYER` = `Backend` | `Frontend`.

### Step 2B.3 — Select sub-task

- **Backend** → sub-task with a Backend layer label (see assignee-rules).
- **Frontend** / **UI** → sub-task with a Frontend layer label.

If no matching sub-task:

- List all sub-tasks (key, summary, labels, reporter, assignee).
- Ask user once for `layer` or explicit `sub-task CCIPH-####`.
- **Do not** create the bug with story reporter as assignee unless user explicitly confirms.

### Step 2B.4 — Resolve accountId for assignee

```text
ASSIGNEE_ACCOUNT_ID = subTask.reporter.accountId
```

If reporter absent or service account → use `subTask.assignee.accountId`.

Store for Phase 4: `SOURCE_SUBTASK_KEY`, `ASSIGNEE_DISPLAY_NAME`, `DEFECT_LAYER`.

---

## Phase 3 — Duplicate check

**Identical** to [qa-jira-defect-logger Phase 3](../qa-jira-defect-logger/SKILL.md#phase-3--duplicate-check).

---

## Phase 4 — Create or update Jira issue

### Option A — Comment on existing bug

Same as parent skill; append assignee context only if reassigning is requested.

### Option B — Create new bug

Same as parent skill **plus**:

| Parameter | Value |
|-----------|-------|
| `description` | [defect-template.md](defect-template.md) including **Ownership** table |
| `additional_fields` | `labels`: include `automation`, `katalon`, `<TC_ID>`, and `backend` or `frontend` (lowercase) |
| `assignee` | `{ "id": "<ASSIGNEE_ACCOUNT_ID>" }` when supported |

**Summary** unchanged: `[Automation] <TC_ID>: <short root cause>`

After `createJiraIssue`, if assignee was not set:

- `editJiraIssue` with `assignee: { id: ASSIGNEE_ACCOUNT_ID }`

### Phase 4C — Log error comment (mandatory)

Same as parent [qa-jira-defect-logger Phase 4C](../qa-jira-defect-logger/SKILL.md#phase-4c--log-error-comment-mandatory): after create or on duplicate bug, post [log-error-comment-template.md](../qa-jira-defect-logger/log-error-comment-template.md) via `addOrEditJiraIssueComment`.

### Link to related story

Same as parent: `createIssueLink` Bug ↔ `STORY_KEY` (`Relates` or project-standard type).

**Do not** link the bug to the sub-task unless the user asks; mention sub-task key in description only.

---

## Phase 5 — Deliver

Same as parent skill, **plus** this block:

```markdown
### Developer assignee (sub-task)

| Field | Value |
|-------|-------|
| Defect layer | Frontend \| Backend |
| Sub-task | CCIPH-#### |
| Assignee from | Sub-task Reporter (or Assignee fallback) |
| Assignee | <name> |
```

Footer in comments: `Logged via qa-jira-defect-logger-subtask-assignee`.

---

## Error handling

Parent skill table applies, plus:

| Situation | Action |
|-----------|--------|
| No sub-tasks on story | List story children; ask user for sub-task key or confirm story-level assignee |
| Sub-tasks exist but no Backend/Frontend labels | Show sub-task table; ask user to pick sub-task or add labels in Jira |
| Assignee field read-only | Create bug unassigned; comment with intended assignee + sub-task; note in delivery |
| Layer ambiguous (API + UI) | Ask user `layer backend` or `layer frontend` once |

## Do not

- Use parent **User Story** reporter as default assignee
- Use **issue links** to other stories as assignee source (only **parent sub-tasks**)
- Assign Backend bugs from Frontend-labeled sub-tasks (or the reverse)
- Skip Phase 2B when creating a new bug

## Quick reference — MCP tools

| Step | Tool |
|------|------|
| Sub-tasks | `searchJiraIssuesUsingJql`, `getJiraIssue` |
| Create / assign | `createJiraIssue`, `editJiraIssue` |
| All other steps | Same as [qa-jira-defect-logger](../qa-jira-defect-logger/SKILL.md#quick-reference--mcp-tools) |

## Related files

| File | Purpose |
|------|---------|
| [assignee-rules.md](assignee-rules.md) | Label matching, layer inference, assignee fallback |
| [defect-template.md](defect-template.md) | Description + Ownership section |
| [../qa-jira-defect-logger/SKILL.md](../qa-jira-defect-logger/SKILL.md) | Base failure parse, duplicate check, linking |
