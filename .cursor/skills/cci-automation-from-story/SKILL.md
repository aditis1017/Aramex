---
name: cci-automation-from-story
description: >-
  Generates Katalon Web UI automation from a CCIPH User Story by discovering manual
  test cases from qa-manual-test-case (linked Test Cases) and/or cci-manual-tc-subtask-xlsx
  (Sub-tasks). Uses FE/BE code under .cursor/cci-skills for locators and Keywords for
  methods. Invoke with /cci-automation-from-story CCIPH-### or automate CCIPH-### when
  this skill is attached.
---

# CCI Automation from Story — Katalon + FE/BE accuracy

Generate **production-ready Katalon** assets (Test Cases, Scripts, Object Repository, Keywords, Suite) from a **CCIPH User Story** and its **existing** manual tests — with **locators and methods grounded in application source**.

## Relationship to other skills

| Upstream (manual TCs) | This skill |
|----------------------|------------|
| [qa-manual-test-case](../../skills/qa-manual-test-case/SKILL.md) — linked **Test Case** issues | Discovers via `issuelinks` |
| [cci-manual-tc-subtask-xlsx](../../cci-skills/cci-manual-tc-subtask-xlsx/SKILL.md) — **Sub-task** + xlsx | Discovers via `parent = STORY` + `manual-test` label |
| [co-pilot-automation-skill](../../skills/co-pilot-automation-skill/SKILL.md) | Same Katalon rules; this skill adds **cci-skills FE/BE** + dual discovery |

**Input:** only `CCIPH-###` (same key used for manual generation).  
**Do not** ask for TC keys, CSV, or a full inputs form.

## Rules & context (load before coding)

| Resource | Use |
|----------|-----|
| [framework-structure.md](framework-structure.md) | Folders, reuse order, encoding, login |
| [jira-test-discovery.md](jira-test-discovery.md) | Find Test Cases + Sub-tasks |
| [fe-be-locator-playbook.md](fe-be-locator-playbook.md) | FE grep → OR; BE → assertions |
| [jira-post-update.md](jira-post-update.md) | Labels `automation`, xlsx columns |
| [co-pilot-automation-skill/SKILL.md](../../skills/co-pilot-automation-skill/SKILL.md) | Waits, screenshots, negative UI rules, suite naming |
| [qa-manual-test-case/ui-product-context.md](../../skills/qa-manual-test-case/ui-product-context.md) | Dev URL, Figma, modules |
| [cci-testcase.mdc](../../rules/cci-testcase.mdc) | Test-type awareness |
| `.cursor/cci-skills/Required Files/` | Application + architecture context |
| `qa/traceability/module-map.yaml` | Module ↔ FE ↔ BE |
| [cci-e2e-qa-pipeline/environments.md](../cci-e2e-qa-pipeline/environments.md) | Dev / staging URLs per module |

**Application source roots (mandatory for new locators):**

- `.cursor/cci-skills/FE-ApplicationCode/`
- `.cursor/cci-skills/BE-ApplicationCode/services/`

## Invocation

| Message | Action |
|---------|--------|
| `/cci-automation-from-story CCIPH-385` | Full generation |
| `Automate CCIPH-385` (skill attached) | Same |
| `Generate Katalon for CCIPH-30 sub-tasks` | `subtasks-only` mode |
| `CCIPH-385 linked test cases only` | `linked-only` mode |

### Optional overrides (chat)

| Override | Example |
|----------|---------|
| Selection | `keys CCIPH-501, CCIPH-502` / `prefix TC_GS_` / `required` |
| Source | `linked-only` / `subtasks-only` |
| Environment | `environment dev` (default) \| `environment staging` |
| Base URL | `url https://dev-cc.aramex.com/customs-management` (overrides env table) |
| Module | `module Global Settings` |
| Suite | `suite MY_SUITE` / `no suite` |
| Skip Jira update | `no jira update` |

### Defaults

| Setting | Value |
|---------|--------|
| Project | `CCIPH` |
| Environment | **`dev`** unless `environment staging` in chat — see [environments.md](../cci-e2e-qa-pipeline/environments.md) |
| Base URL | From environments.md for resolved env (dev default: `https://dev-cc.aramex.com/customs-management`) |
| Browser | Chrome (suite) |
| Selection | `ALL` (linked + sub-task manual tests, deduped) |
| MODULE_ROOT | Story summary before `\|` |
| Suite | `CCI_<MODULE_ABBR>_<STORY>_SUITE` |
| Login in feature TCs | Off (preconditions only) |

---

## Workflow (mandatory order)

### Step 0 — Validate

- Story key `CCIPH-\d+`.
- Atlassian MCP **`user-atlassian`** connected; else **STOP**.
- `getAccessibleAtlassianResources` → `cloudId`.
- Resolve **environment** (`dev` \| `staging`) and **BASE_URL** / **MODULE_URL** per [environments.md](../cci-e2e-qa-pipeline/environments.md).

### Step 1 — Fetch story

`getJiraIssue` with summary, description, issuelinks, subtasks, labels, components, attachments.

Derive **MODULE_ROOT**, **MODULE_ABBR**, suite name.

### Step 2 — Discover test specifications

Follow [jira-test-discovery.md](jira-test-discovery.md):

1. Collect **Upstream A** (linked Test Cases).
2. Collect **Upstream B** (manual-test Sub-tasks).
3. Dedupe by **TC_ID**; if duplicate A+B, prefer description with richer steps; keep both Jira keys in script comment if needed.
4. Apply selection filters.

If **zero** specs: report upstream commands; stop unless `scaffold from AC`.

### Step 3 — Coverage report (chat, before files)

```text
Story: CCIPH-xxx | <summary>
MODULE_ROOT: ...
Discovery: N linked Test Cases, M sub-tasks → K unique TC_IDs
FE path: app/customs-management/...
BE services: persistence, ...
Reuse: <count> existing OR, <count> keyword methods
Automating: list TC_IDs + Jira keys
Suite: CCI_..._SUITE.ts
```

### Step 4 — FE/BE grounding (per module, once per run)

1. Resolve routes/components from [module-map.yaml](../../../qa/traceability/module-map.yaml) + FE qa README.
2. Grep FE components for `aria-label`, labels, roles (see playbook).
3. List existing `Object Repository/<MODULE_ROOT>/` and `Keywords/` hits to reuse.
4. Note BE services for expected messages (grep if ER mentions validation text).

**Hard rule:** Do not add new OR entries without FE evidence **or** an existing OR pattern in the same module.

### Step 5 — Generate Katalon assets

For **each** kept specification:

1. **Object Repository** — `Object Repository/<MODULE_ROOT>/<STORY>/` — one entry per control; XPath/CSS from playbook; enable smart locator when supported.
2. **Keywords** — extend module `BusinessActions.groovy` (or create per [framework-structure.md](framework-structure.md)) with navigation/actions used by ≥2 TCs.
3. **Script** — `Scripts/<MODULE_ROOT>/<STORY>/TC_<TC_ID>_<slug>.groovy` — thin: keywords + waits + verify + screenshots.
4. **Test Case** — `Test Cases/<MODULE_ROOT>/<STORY>/` — link script; no login steps in body.

Apply **co-pilot-automation-skill** rules: waits, UTF-8 no BOM, single ER assertion, UI negative cases, screenshot strategy.

Script header:

```groovy
// Jira TC: CCIPH-### | Story: CCIPH-### | TC_ID: TC_GS_FUNC_001
// cci-automation-from-story | Upstream: sub-task|linked-test-case
```

### Step 6 — Test Suite

Create/update `Test Suites/CCI_<MODULE_ABBR>_<STORY>_SUITE.ts` including all new `.tc` files unless `no suite`.

Document: login via separate suite / manual session.

### Step 7 — Jira post-update

Unless `no jira update`, for each automated Jira issue follow [jira-post-update.md](jira-post-update.md): labels + comment with paths.

Update xlsx CSV mirror when present.

### Step 8 — Deliver

```markdown
## CCI Automation Complete

**Story:** CCIPH-xxx
**Specs automated:** K (list TC_ID → Jira key → .tc path)
**New OR:** count | **Reused OR:** count
**Keywords touched:** paths
**Suite:** Test Suites/...
**FE references:** list files grep'd
**Next:** Run suite in Katalon; then qa-jira-suite-result-updater CCIPH-xxx
**Defects:** qa-jira-defect-logger-subtask-assignee or qa-jira-defect-logger
```

---

## Do not

- Invent test steps not in Jira spec (or xlsx row for that TC_ID)
- Use story reporter for defects (use defect-logger skills)
- Duplicate login/locators across scripts
- Use API/HTTP assertions for standard Web UI cases
- Modify existing `.cursor/skills/co-pilot-automation-skill/SKILL.md` (this skill supersedes for cci-skills FE/BE runs)

## MCP tools

| Step | Tool |
|------|------|
| Story / TC / sub-task | `getJiraIssue`, `searchJiraIssuesUsingJql` |
| Labels / comment | `editJiraIssue`, `addOrEditJiraIssueComment` |
| Link types (if needed) | `getIssueLinkTypes` |

## Handoff from manual skills

| After manual skill | Run |
|--------------------|-----|
| `/qa-manual-test-case CCIPH-xxx` | `/cci-automation-from-story CCIPH-xxx` (`linked-only` optional) |
| `/cci-manual-tc-subtask-xlsx CCIPH-xxx` | `/cci-automation-from-story CCIPH-xxx` (`subtasks-only` optional) |

Mention this handoff in `qa-manual-test-case` delivery is optional; user uses this skill explicitly.
