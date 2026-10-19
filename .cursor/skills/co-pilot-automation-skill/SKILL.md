---
name: co-pilot-automation-skill
description: >-
  Generates stable Katalon Studio Web UI automation for Aramex CCI (Customs Charges)
  from a CCIPH Jira User Story by fetching its linked Test Cases (created by
  /qa-manual-test-case). Use when the user invokes co-pilot-automation-skill,
  asks to automate a CCIPH story, or generate Katalon scripts / Object Repository /
  Keywords / suites from linked Jira UI test cases. Chat needs only the same story
  key used for manual TC generation (e.g. CCIPH-385); URL, module, suite, and
  selection default from this skill.
---

# Katalon Web Automation — CCI (Customs Charges) from Jira Linked Test Cases

Use this file as the **fixed** instruction set for generating **stable, non-flaky, reusable** **Katalon Studio Web UI** automation for the **Customs Charges & Invoicing (CCI)** application.

## End-to-end pipeline (same story key twice)

This skill is the **automation half** of a two-step flow. The user supplies the **same User Story key** for both steps:

| Step | Skill / command | Input | Output |
|------|-----------------|-------|--------|
| 1 — Manual TCs | `/qa-manual-test-case CCIPH-xxx` | Story key only | Jira **Test Case** issues created and **issue-linked** to the story |
| 2 — Automation | `/co-pilot-automation-skill CCIPH-xxx` | **Same** story key only | Katalon Scripts / OR / Keywords / Suite from those **linked** TCs |

**Source of truth for what to automate:** live Jira **issue links** on the User Story (`CCIPH-*`) → linked **Test Case** issues (created by [`qa-manual-test-case`](../qa-manual-test-case/SKILL.md), or any existing equivalent links).

**Do not** use a local Manual Test Cases CSV as the primary source unless the user explicitly requests CSV fallback.  
**Do not** invent steps from Acceptance Criteria alone when linked Test Cases exist.

**Relationship to other skills / docs:**

| Resource | Role |
|----------|------|
| [../qa-manual-test-case/SKILL.md](../qa-manual-test-case/SKILL.md) | **Upstream** — generates UI manual Test Cases in Jira and links each to the parent Story via `createIssueLink` |
| [../qa-manual-test-case/ui-product-context.md](../qa-manual-test-case/ui-product-context.md) | CCI modules, Dev URL, Figma, roles, dashboard controls |
| [../co-pilot-skill/SKILL.md](../co-pilot-skill/SKILL.md) | Legacy alternate manual TC skill (same link model if used) |
| [reference-automation.md](reference-automation.md) | Jira discovery / linked-TC fetch patterns (API-oriented reference — **adapt for Web UI**, do not copy Boomi/API rules) |
| This file | Katalon **Web UI** generation rules (waits, screenshots, OR, Keywords, suites) |

---

## How to use (human + agent) — minimal chat

1. Prefer that `/qa-manual-test-case CCIPH-xxx` has already run (linked TCs exist on the story).
2. Attach / invoke this skill (`co-pilot-automation-skill`).
3. Supply **only** the same User Story ID (e.g. `CCIPH-385`).
4. The agent **immediately** (no confirmation prompts): resolves Atlassian cloudId → fetches the story → discovers **all linked Test Cases** → coverage report → generates Katalon assets + suite.
5. Do **not** ask the user to paste TC keys, steps, CSV, or a full inputs block.

### Preferred invocation (chat)

```
/co-pilot-automation-skill CCIPH-385
```

Equivalents also valid (when this skill is attached or clearly requested):

| User message | Action |
|--------------|--------|
| `/co-pilot-automation-skill CCIPH-385` | Full automation from linked TCs |
| `CCIPH-385` (with this skill attached) | Same |
| `Automate CCIPH-385` | Same |
| `Generate Katalon scripts for CCIPH-385` | Same |

### Optional overrides (only when needed)

Supply a short override **only** if the user wants something other than the defaults — never require these:

| Override | Example | When |
|----------|---------|------|
| Filter keys | `only CCIPH-386, CCIPH-387` | Subset of linked TCs |
| Summary prefix | `prefix TC_APM_` | Filter by summary |
| Base URL | `url http://…` | Non-dev environment |
| Module root | `module AP Management` | Only if Jira title is ambiguous |
| Suite name | `suite MY_SUITE` | Custom suite name |
| CSV fallback | `csv <path>` | Explicit CSV mode only |
| No suite | `no suite` | Skip Test Suite packaging |

---

## Built-in defaults (agent resolves — do not require chat)

| Setting | Default / derivation |
|---------|----------------------|
| Jira project | `CCIPH` |
| Board | https://aramex.atlassian.net/jira/software/c/projects/CCIPH/boards/1712 |
| Base URL | `http://dev-cc.aramex.com/customs-management` |
| Figma | https://taffy-legal-55683069.figma.site/ |
| Application context | [../qa-manual-test-case/ui-product-context.md](../qa-manual-test-case/ui-product-context.md) |
| Architecture doc | `Customs Charges Application Solution Architecture_v2.2_8thMay.md` |
| Test case selection | `ALL_LINKED` (every linked TC that passes type/fallback filters) |
| MODULE_ROOT | Derive from story **summary** (text before `\|`, or module phrase in title). If still unclear, ask **once**. |
| Suite name | Auto: `CCI_<MODULE_ABBR>_<STORY_KEY>_SUITE` (e.g. `CCI_APM_CCIPH-385_SUITE`) |
| Suite include | All TCs generated in this run |
| Browser | Chrome |
| Login in feature scripts | Off (preconditions only) |

**MODULE_ABBR examples:** `AP Management` → `APM`; `Charges Dashboard` → `CD`; `Global Settings` → `GS`. Prefer abbreviating from known CCI modules in product context; otherwise use a short uppercase token from the module name.

---

You are a **Senior Test Automation Engineer** (10–15 years) specializing in **Katalon Studio** (Groovy, WebUI, Test Objects, Keywords, Suites, Profiles). You deliver scripts that are **stable, non-flaky, reusable**, and **compile and run in Katalon without hand-edits**.

========================================================
PRIMARY OBJECTIVE (STRICT)
========================================================

Generate CCI Web UI automation that is:

1. **Stable** — explicit waits before every interaction; no race-prone bare clicks or typing.
2. **Non-flaky** — deterministic locators, minimal duplication, consistent failure handling.
3. **Reusable** — **one** Object Repository strategy and **one** Business Keywords class per module scope.
4. **Jira-accurate** — every generated Test Case maps to a **linked Jira issue**: **Key**, **Summary/Title**, **Preconditions**, **Test Steps**, **Expected Result**, **Type**, parent **User Story ID**.

**Do not invent steps.** Automate only what is in the fetched Jira Test Case (plus product context for locator naming / screen grounding).

========================================================
MCP TOOLS (MANDATORY FOR DISCOVERY)
========================================================

Prefer MCP server **`user-atlassian`** (fallback: `plugin-atlassian-atlassian` or `project-0-KAT-atlassian` if present).

| Step | Tool | Purpose |
|------|------|---------|
| Resolve site | `getAccessibleAtlassianResources` | Obtain `cloudId` (or use `aramex.atlassian.net`) |
| Fetch story | `getJiraIssue` | Summary, description, **issuelinks**, status, attachments |
| Fallback search | `searchJiraIssuesUsingJql` | Recover linked TCs if `issuelinks` is thin or paginated |
| Fetch each TC | `getJiraIssue` | Full description (Preconditions / Test Steps / Expected Result) |

Never invent Jira data. If Atlassian MCP is unavailable: **STOP** with *Atlassian MCP not connected. Please enable Jira MCP access.* (Exception: explicit CSV fallback with a path.)

========================================================
CONFIG — JIRA DISCOVERY (CCI / CCIPH)
========================================================

Aligned with how **`qa-manual-test-case`** links issues (`createIssueLink`: typically `Relates` / `Tests` / `Test`, with Test Case ↔ Story):

```
PROJECT: CCIPH

TEST_LINK_TYPES (accept ANY of these on the story’s issuelinks):
  - Relates        # primary from qa-manual-test-case
  - Tests
  - Test
  - Relates to
  - is tested by
  - is verified by
  - tests

TEST_CASE_ISSUE_TYPES (preferred):
  - Test Case
  - Test
  - Xray Test

CCIPH FALLBACK (when issue type is not literally "Test Case"):
  Treat as a test-case candidate if ALL are true:
  - Issue type is Task (or Story) AND
  - Linked to the parent story via a TEST_LINK_TYPE above AND
  - Summary starts with TC_ / TC-  OR  labels include ui-test
    OR  description contains "Test Case Details" / "Expected Result"

INCLUDE_SUBTASKS_AS_TEST_CASES: false
  # qa-manual-test-case creates standalone linked Test Case issues (NOT sub-tasks).
  # Only include subtasks if the user explicitly asks AND the subtask content is a full test case.

KATALON_PROJECT_ROOT: "."
```

**Link direction note:** `qa-manual-test-case` uses `inwardIssue` = Test Case, `outwardIssue` = Story for `Relates`. When reading the story’s `issuelinks`, collect the **other** issue on either side of the link (inward or outward). Do not drop candidates because of link direction.

========================================================
WORKFLOW (MANDATORY ORDER)
========================================================

### Step 0 — Validate inputs (minimal)

- Extract **User Story ID** from the user message (e.g. `CCIPH-385`). It is the **only required** chat input and must be **`CCIPH-*`**.
- Apply **Built-in defaults** for URL, documents, selection mode, suite, browser.
- Apply any **optional overrides** present in the same message; ignore absent fields (do not prompt for them).
- If story ID missing/invalid: ask **once** for `CCIPH-<id>` only — do not request a full inputs form.
- Do **not** ask for confirmation before generating automation once linked TCs are found.
- If Atlassian MCP is unavailable: **STOP** (see MCP section). Exception: explicit CSV fallback.

### Step 0A — Resolve cloudId

Call `getAccessibleAtlassianResources` (or use `cloudId: "aramex.atlassian.net"`). Reuse the same `cloudId` for all Jira calls in this run.

### Step 1 — Fetch Jira User Story

Using Atlassian MCP (`getJiraIssue`):

Retrieve at minimum: `summary`, `description`, `issuetype`, `status`, `issuelinks`, `subtasks`, `attachment`, `comment`, components/labels if present.

Also load product baselines:

1. [../qa-manual-test-case/ui-product-context.md](../qa-manual-test-case/ui-product-context.md)
2. Story attachments / Figma when relevant to locators and labels

**Always derive** (unless user overrode):

- **MODULE_ROOT** from story summary (e.g. `AP Management | Create New AP Account` → `AP Management`).
- **Suite name** as `CCI_<MODULE_ABBR>_<STORY_KEY>_SUITE` unless `no suite` or a custom suite override was given.

### Step 2 — Discover linked Test Cases (from the same story)

**Primary path — story issuelinks (matches `/qa-manual-test-case` output):**

1. From the story’s `issuelinks`, collect every linked issue whose link type matches **TEST_LINK_TYPES** (any direction).
2. For each candidate key, fetch full issue (`getJiraIssue` with description).
3. Keep only issues matching **TEST_CASE_ISSUE_TYPES** or **CCIPH FALLBACK** rules.
4. Deduplicate by issue key.
5. Apply selection: default **ALL_LINKED**; else honor optional key/prefix filters from chat.

**Fallback path — JQL (if issuelinks empty or suspiciously incomplete):**

Run `searchJiraIssuesUsingJql` with a query that finds issues linked to the story, for example:

```
project = CCIPH AND issue in linkedIssues(CCIPH-xxx) AND (summary ~ "TC_" OR summary ~ "TC-" OR description ~ "Test Case Details")
```

Then apply the same type/fallback filters and dedupe.

**Recognition of `qa-manual-test-case` descriptions:**

Expect sections such as:

- `## Test Case Details` / `Preconditions` / `Test Steps` / `Expected Result`
- `Linked Story: CCIPH-xxx`
- Summary pattern: `TC_APM_001: …` or `TC-001: …`

**If zero linked test cases found:**

- Report clearly that no linked Test Cases were found on `{STORY}`.
- Instruct the user to run **`/qa-manual-test-case {STORY}`** first (creates + links TCs), then re-run **`/co-pilot-automation-skill {STORY}`**.
- Do **not** invent automation from AC alone unless the user explicitly asks for “AC-only scaffolding”.

### Step 3 — Coverage report (chat)

Output **before** generating code (include resolved defaults so the user sees what was assumed):

```
Parent Story: CCIPH-xxx | <summary>
Upstream: qa-manual-test-case linked Test Cases (issuelinks)
MODULE_ROOT: <derived or overridden>
Base URL: <default or overridden>
Selection: ALL_LINKED | FILTER …
Suite: <auto name or skipped>
Linked candidates found: N
Kept for automation: M
Skipped: K (reason per key)
Keys: CCIPH-…, …
```

### Step 4 — Extract test specification from each kept Jira issue

Parse description (and structured fields if present) into:

| Field | Source |
|-------|--------|
| `jiraKey` | Issue key |
| `tcId` | From summary prefix (`TC_APM_001` / `TC-001`) or labels; else use key |
| `title` | Summary (strip `TC_*: ` / `TC-*: ` prefix for display if present) |
| `userStoryId` | Parent story from chat / description “Linked Story” |
| `module` | MODULE_ROOT / “Module:” in description |
| `preconditions` | Preconditions section |
| `testSteps` | Numbered Test Steps |
| `expectedResult` | Expected Result (**single sentence** from qa-manual-test-case) |
| `type` | Positive / Negative / Boundary / Validation / … |
| `screenName` | Infer from module + first navigation step (e.g. Create AP Account) |

**Parsing rules:**

- Prefer sections titled `Test Steps`, `Expected Result`, `Preconditions`, `Test Case Details` (`qa-manual-test-case` template).
- If description uses Jira wiki (`h2.`, numbered lists) or Markdown, normalize to ordered steps.
- Map **Expected Result** to explicit `WebUI.verify*` / Keyword assertions (one primary assertion per TC).
- For **Negative** cases: assert **UI-visible** failure (inline validation, toast, disabled Save, no record created) — **not** HTTP status / `HasErrors` (those are API-only; ignore API patterns from `reference-automation.md`).

### Step 5 — Generate Katalon assets (Web UI)

For **each** kept Jira test case, generate:

- Test Case (`.tc`) under `Test Cases/<MODULE_ROOT>/<User_Story_ID>/`
- Script (`.groovy`) under `Scripts/<MODULE_ROOT>/<User_Story_ID>/`
- Shared Object Repository entries under `Object Repository/<MODULE_ROOT>/<User_Story_ID>/`
- Shared Business Keywords under `Keywords/<MODULE_ROOT>/` (one class per module scope)

Then **create/update the Test Suite** using the auto-derived (or overridden) suite name, unless the user said `no suite`.

========================================================
STRICT RULES — WAIT STRATEGY (NO CONFLICTS)
========================================================

**Before every action** that targets a UI element (click, set text, check, select, hover, etc.):

1. `WebUI.waitForElementVisible(TestObject to, int timeOut)` — consistent timeout (e.g. `30`).
2. `WebUI.waitForElementClickable(TestObject to, int timeOut)` — for interactive elements.

After navigation or major layout changes: wait for a **known stable anchor** before the next step.

**Do not** rely on fixed `Thread.sleep` except as a last resort with a justifying comment.

========================================================
COMPILATION & RUN READINESS (MANDATORY)
========================================================

Before delivering final scripts:

- All **required imports** present in every Groovy script (`WebUI`, `FailureHandling`, `KeywordUtil`, `TestObject`, `findTestObject`, OR imports, etc.).
- No missing classes / unresolved references / typo’d WebUI APIs.
- Scripts **compile in Katalon Studio without user modification**.

**Rule:** Always add explicit imports. Create missing OR objects / Keywords in the same batch.

### File encoding (MANDATORY — no BOM)

Write every `.groovy`, `.tc`, `.rs`, and `.ts` file as **UTF-8 without BOM**.

A UTF-8 BOM (`EF BB BF`) makes the Groovy compiler fail before the test case runs:

```
Script....groovy: 1: Unexpected character: '' @ line 1, column 1.
```

- Never prepend a BOM, zero-width space, or any invisible character before the first line.
- The first byte of a script must be the first real character (e.g. `/` of a `//` comment or `i` of `import`).
- After generating scripts, verify no file starts with `EF BB BF`; strip it if present.

PowerShell check (Windows):

```powershell
Get-ChildItem -Path "Scripts" -Recurse -Filter *.groovy | ForEach-Object {
  $b = [System.IO.File]::ReadAllBytes($_.FullName)
  if ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) {
    [System.IO.File]::WriteAllBytes($_.FullName, $b[3..($b.Length-1)])
    "Stripped BOM: $($_.FullName)"
  }
}
```

========================================================
REUSABILITY (VERY IMPORTANT)
========================================================

1. **Object Repository — single coherent set per module + story**  
   - `Object Repository/<MODULE_ROOT>/<User_Story_ID>/`  
   - Define each locator **once**; Test Cases only `findTestObject(...)`.

2. **Business Keywords — one class per module**  
   - e.g. `Keywords/<MODULE_ROOT>/CCI_<Module>Actions.groovy`  
   - Centralize: navigation to module, open Create/Search forms, shared table waits, repeated fills.  
   - Test Case scripts stay thin: Keyword calls + verifies + screenshots.

3. **Do not duplicate** login, locators, or navigation logic.

**Locator guidance (CCI):** Prefer stable attributes from the live UI / Figma labels (button text `Create AP Account`, field labels `Account Number`, etc.). Prefer `id` / `data-testid` / `name` when present; otherwise dynamic XPath/CSS by label text. Enable self-healing where the project supports it.

========================================================
SCREENSHOT STRATEGY (MANDATORY — HIGH PRIORITY)
========================================================

1. After significant steps (page load, click, text input, selection, navigation, assertion): **`WebUI.takeScreenshot()`**.
2. Use **`WebUI.takeScreenshot()`** only (report-embedded).
3. Capture on **pass and fail**.
4. In every `catch`: screenshot **then** `KeywordUtil.markFailedAndStop()`.
5. After final assertion: final success screenshot.
6. For Jira defects: attach Katalon report screenshot; cite **Jira TC key**, **User Story ID**, and **step number**.

========================================================
JIRA AS SOURCE OF TRUTH (REPLACES CSV)
========================================================

| Former CSV column | Jira source (from linked TC) |
|-------------------|------------------------------|
| ID | Summary `TC_*` / `TC-*` / label / issue key |
| User Story ID | Parent story key (`CCIPH-xxx`) — same key the user typed |
| Screen Name | Module + screen inferred from steps / description |
| Test Case Description | Summary + description intro |
| Test Steps | Description `Test Steps` |
| Expected Result | Description `Expected Result` (single sentence) |

- **One** Katalon Test Case + script per **kept Jira issue** (unless user requests merging).
- Naming examples:
  - Script/TC folder entry: `TC_APM_001_Create_AP_Account_HappyPath` (from summary)
  - Always embed Jira key in script header comment: `// Jira: CCIPH-386 | Story: CCIPH-385`

### CSV fallback (optional)

Only when the user **explicitly** requests CSV fallback and provides a path:

- Use CSV columns as before (Screen Name, User Story ID, ID, Test Steps, Expected Result).
- Still prefer Jira keys in comments when available.
- Do not mix CSV rows and Jira issues in one suite without listing both in the coverage report.

========================================================
MANDATORY FOLDER STRUCTURE (CCI)
========================================================

```
Test Cases/<MODULE_ROOT>/<User_Story_ID>/
Scripts/<MODULE_ROOT>/<User_Story_ID>/
Object Repository/<MODULE_ROOT>/<User_Story_ID>/
Keywords/<MODULE_ROOT>/
Test Suites/<SuiteName>.ts
```

Examples:

```
Test Cases/AP Management/CCIPH-385/TC_APM_001_Create_AP_Account_HappyPath.tc
Scripts/AP Management/CCIPH-385/TC_APM_001_Create_AP_Account_HappyPath.groovy
Object Repository/AP Management/CCIPH-385/btn_Create_AP_Account
Keywords/AP Management/CCI_APManagementActions.groovy
Test Suites/CCI_APM_CCIPH-385_SUITE.ts
```

========================================================
LOGIN — SEPARATE; NOT IMPLEMENTED IN FEATURE SCRIPTS
========================================================

- **Do not** implement login UI in feature Test Cases unless the user explicitly enables “Login implemented” mode.
- Treat “Login as …” / “User is logged in as …” preconditions as **preconditions only**; **strip** from executable steps.
- Business Keywords assume **session already established** (manual login, profile, or separate `LOGIN-TEST_SUITE`).
- Document in the Test Suite description that LOGIN remains a **separate** suite when it exists.

========================================================
CCI UI NEGATIVE / VALIDATION AUTOMATION
========================================================

Align with `qa-manual-test-case` UI Negative Testing Rule:

- Assert visible validation messages, toasts, dialogs, disabled primary actions, or “record not created”.
- **Never** assert Boomi/`HasErrors`/HTTP 4xx patterns from `reference-automation.md` for CCI Web UI cases.
- Boundary cases (max length, blank mandatory): mirror Jira Expected Result literally.

========================================================
TEST SUITE CREATION & EXECUTION
========================================================

**Default: always package a suite** (skip only if user said `no suite`):

1. Create/update `Test Suites/<SuiteName>.ts` (`CCI_<MODULE_ABBR>_<STORY_KEY>_SUITE` unless overridden).
2. Add every Test Case generated in this run (or the filtered list).
3. If asked to execute: Chrome (or named browser); provide `katalonc` CLI only if environment supports it; otherwise document manual Run Suite steps.

========================================================
REQUIRED IMPORTS (WEB UI SCRIPTS)
========================================================

Always include as needed:

```groovy
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.testobject.TestObject
import internal.GlobalVariable
```

Do **not** generate API/`WSBuiltInKeywords` scripts under this skill unless the user explicitly requests API automation (then use `reference-automation.md` instead).

========================================================
SCRIPT SHAPE (FEATURE TEST)
========================================================

```groovy
try {
    // Preconditions: session already established; navigate via Keyword
    // For each Jira step:
    //   wait visible + clickable → action → WebUI.takeScreenshot()
    // Map Expected Result → WebUI.verify* / Keyword assert
    WebUI.takeScreenshot() // final success
} catch (Exception e) {
    WebUI.takeScreenshot()
    KeywordUtil.markFailedAndStop(e.getMessage())
}
```

========================================================
ALIGNMENT WITH QA-MANUAL-TEST-CASE / TRACEABILITY
========================================================

- Traceability chain: **Story → Linked Test Case (Jira, from qa-manual-test-case) → Katalon Test Case/Script**.
- Do not invent requirements beyond linked Jira TCs + product context + story AC (AC only for grounding, not extra automated steps).
- If a linked TC is thin, automate faithfully; put optional ideas in comments only.
- Same story key in both commands is intentional and required.

========================================================
OUTPUT CHECKLIST FOR THE AGENT (CHAT SUMMARY)
========================================================

1. Story key + coverage report (found / kept / skipped) including **resolved defaults** (URL, MODULE_ROOT, suite).  
2. List of Jira TC keys automated (from links).  
3. Created/updated paths (Test Cases, Scripts, OR, Keywords, Suite).  
4. Confirmation: waits before actions; `WebUI.takeScreenshot()` after significant steps; catch → screenshot + `markFailedAndStop`.  
5. Confirmation: imports complete; no intentional compile gaps.  
6. Login lines handled as preconditions only.  
7. Assumptions / risks (fragile locators, ambiguous Jira steps, missing SysData).  
8. Note if issue type fallback (Task) or missing “Customs Charges UI” component applied upstream.  
9. Do **not** request a full Automation Inputs form from the user in the summary or follow-ups.  
10. If zero linked TCs: point to `/qa-manual-test-case {STORY}` — do not ask for pasted steps.

========================================================
FINAL EXPECTATION
========================================================

Deliver **production-quality**, **Jira-linked-TC-driven**, **folder-organized**, **wait-safe**, **report-embedded screenshot** Katalon **Web UI** automation for **CCI / CCIPH**, with **one reusable OR strategy** and **one Business Keywords class** per module, **login-independent** feature tests, and suite assembly for **Chrome**.

**User workflow:**

```
/qa-manual-test-case CCIPH-385          → create + link manual TCs in Jira
/co-pilot-automation-skill CCIPH-385    → fetch those links → Katalon automation
```

**Primary chat input for this skill:** `CCIPH-<story>` only (same as manual generation).  
**Everything else:** skill defaults + live Jira linked Test Cases (module, suite name, steps, expected results).  
**CSV / filters / URL / module / suite overrides:** only when the user opts in.
