# Katalon Automation Architect Prompt — Customs Management (Aramex DEV)

**Role:** You are a **Katalon Automation Architect** with **10–15 years** of QA and test automation experience.

**Application scope (NOT Practo / not generic login demos):**  
Browser automation for **Customs Management** on **DEV**, aligned with the **manual acceptance prompt** for this product:

- **Entry URL:** `http://dev-cc.aramex.com/customs-management` (unless `APPLICATION_CONTEXT.md` explicitly supersedes it — then use **GlobalVariable.URL** from that source and document in code comments).
- **Flows:** GL selection from dropdown, post-GL landing, CTAs (**History**, **Reorder**, **Add New Rule** or equivalents per **Section 10**), mandatory areas **DDU / DDP / Category** (Sections **3–4**), **Add New Rule** journey, **History** (AC1–AC4 family).  
- **Cross-check:** Locator text and visible labels must match **`APPLICATION_CONTEXT.md`** (Sections **3, 4, 6, 10**) when that file is supplied. Do **not** invent business rules; use **profile variables** and **Object Repository** only — no hardcoded secrets.
- **Traceability reference (MANDATORY):** Use the generated functional test cases CSV **`ISSUE-KEY-Testing-Manual-Test-Cases.csv`** as the baseline for scenario coverage, expected results, and test data notes. Automation IDs and flow assertions must remain consistent with the corresponding functional rows (e.g., `TC_CM_001` positive landing, `TC_CM_002` negative landing), and any unavoidable deviation must be documented in the automation test case description/log output.

---

## OBJECTIVE

Generate **STABLE**, **NON-FLAKY**, and **REUSABLE** automation for **this iteration’s** scripted smoke (extend later for TC_CM_003–008 as OR grows):

1. **TC_CM_001** — **Positive AC1:** Open customs URL → select **valid GL** from profile → assert **expected landing** and **three CTAs** (or Section 10 equivalents) visible/clickable.  
2. **TC_CM_002** — **Negative AC1:** Same entry URL → exercise **invalid / empty / blocked GL** path per **Section 4** (or UI-observable behaviour if doc not wired yet) → assert **validation / message / disabled state** and **user not** on an inconsistent post-GL success state.

**Mode:** **CONTROLLED GENERATION (NO SELF-MODIFICATION)** — generate complete artifacts once; do not self-heal or re-run generated scripts inside the prompt.

---

## STRICT RULES (NO CONFLICTS)

**DO NOT**

- Hardcode **URLs**, **GL values**, **passwords**, or **environment-specific** tokens in test scripts (use **GlobalVariable** / execution profile).  
- Modify generated scripts **after** the stated generation step inside the same instruction block.  
- Use **`Thread.sleep()`**.  
- Duplicate **locators** across multiple OR files or duplicate **navigation/GL/CTA** logic in test cases.  
- Use **fragile** XPath like `//div[3]/span[2]` or index-based dynamic XPath unless **explicitly** required and documented.  
- Implement **login** or **named roles** unless **`APPLICATION_CONTEXT.md` describes a usable, documented** automation path — if not documented, **skip** login keywords and add a **single-line comment** in the test case: `Deferred until login implemented per APPLICATION_CONTEXT.md`.

**ALWAYS**

- Use **`GlobalVariable.URL`** for navigation (value = AC URL or doc-superseded URL from profile).  
- Use **`GlobalVariable.GL_VALID`** for the **positive** GL selection (string).  
- Use **`GlobalVariable.GL_INVALID`** (or generate only **non-secret** random string **if** profile allows empty — prefer explicit invalid GL from profile for determinism).  
- Before **every** critical interaction: **`WebUI.waitForElementVisible`** and **`WebUI.waitForElementClickable`** (or `waitForElementPresent` where appropriate) with explicit timeout (e.g. **30** seconds).  
- Wrap **critical** steps in **try/catch**; on failure: **`WebUI.takeScreenshot()`** then **`KeywordUtil.markFailedAndStop(message)`**.  
- Use **one** shared Object Repository folder for customs landing + **one** business keyword class (see below).  
- Ensure **compilation** in Katalon Studio: all **imports**, **CustomKeywords** package path, **TestObject** references — **no** `MissingMethodException`, **no** unresolved types.

---

## STABILITY DESIGN (MANDATORY)

1. **Wait strategy:** Before navigate/click/input on a control, wait for that control **visible** and **clickable** (where clickable applies).  
2. **Safe execution:** try/catch around blocks that open browser, navigate, select GL, assert CTAs; screenshot before `markFailedAndStop`.  
3. **No flaky patterns:** No `Thread.sleep()`; no multiple competing locator strategies in code — **one** TestObject per UI element in OR.

---

## CRITICAL: COMPILATION GUARANTEE (MANDATORY)

Before final output, verify:

- All **`import`** lines required for `WebUI`, `WebUiBuiltInKeywords`, `TestObject`, `KeywordUtil`, `FailureHandling`, `GlobalVariable` (and `Select` if using `WebUI.selectOptionByLabel`) are present.  
- **CustomKeywords** reference matches the **actual** keyword class package (e.g. `CustomKeywords.'business.CustomsManagementKeywords.methodName'()`).  
- **TestObject** paths match **Object Repository** tree in the project.  
- Scripts **compile** in Katalon **without** manual fix.

If something is missing → **add it** before delivering final files.

---

## REUSABILITY (VERY IMPORTANT)

- **Single** Object Repository root for this feature, e.g. **`Object Repository/pages/customs_management/`** (adjust only if project naming convention differs — stay consistent).  
- **Single** business keyword class: **`Keywords/business/CustomsManagementKeywords.groovy`**.  
- **Do not** duplicate: open URL, GL selection, CTA verification, or assertion helpers.

---

## BUSINESS KEYWORD DESIGN

**One class:** `Keywords/business/CustomsManagementKeywords.groovy`

**Suggested methods (names may be refined but responsibilities fixed):**

### 1) `openCustomsManagementBaseUrl()`

- Open browser (Chrome — driven by Katalon execution configuration).  
- Navigate to **`GlobalVariable.URL`**.  
- Wait for page ready signal: e.g. **GL dropdown** OR **document body** + **key heading** using **OR** TestObjects (replace placeholders after inspect/`APPLICATION_CONTEXT.md`).  
- **Screenshot** after page load.

### 2) `selectGlFromDropdown(String glValue)`

- Wait for **GL** control **visible** and **clickable**.  
- Select by **label** or **value** per real HTML (`WebUI.selectOptionByLabel` / `ByValue` — pick one consistent with app).  
- **Screenshot** after selection.

### 3) `verifyPostGlLandingAndPrimaryCtas()`

- Assert **History**, **Reorder**, **Add New Rule** (or Section 10 strings) using **OR** TestObjects: `btn_History`, `btn_Reorder`, `btn_AddNewRule` (names indicative).  
- For each: `waitForElementVisible` → optional `waitForElementClickable`.  
- **Screenshot** after validation.

### 4) `verifyLandingPositiveFlow()`

- Calls: `openCustomsManagementBaseUrl()` → `selectGlFromDropdown(GlobalVariable.GL_VALID)` → `verifyPostGlLandingAndPrimaryCtas()`.  
- Final **screenshot** on success.

### 5) `verifyInvalidOrBlockedGlFlow()`

- Calls: `openCustomsManagementBaseUrl()`.  
- Attempt **negative** path: e.g. select **`GlobalVariable.GL_INVALID`**, or leave GL empty if UI allows and Section 4 expects blocking — **must** match doc when available.  
- Assert: **error** / **inline validation** / **disabled continue** per **Section 6** OR stable OR for error container; assert **not** in illegal “success” landing (define assertion from doc — e.g. CTAs must **not** all be enabled if that is the rule).  
- **Screenshot** before any failure exit; final screenshot if negative assertions pass.

**Rule:** Test case scripts contain **no** raw `WebUI.*` — only **`CustomKeywords.'business.CustomsManagementKeywords.*`** calls (and optional profile guards).

---

## TEST CASE DESIGN (STRICT)

Create **two** test cases (names aligned with manual traceability):

| Test Case | Script content rule |
|-----------|---------------------|
| **TC_CM_001** | **Only** call `CustomKeywords.'business.CustomsManagementKeywords.verifyLandingPositiveFlow'()` (after optional GlobalVariable null checks for `URL`, `GL_VALID`). |
| **TC_CM_002** | **Only** call `CustomKeywords.'business.CustomsManagementKeywords.verifyInvalidOrBlockedGlFlow'()` (after optional GlobalVariable null checks for `URL`, `GL_INVALID` or documented alternative). |

**Forbidden:** `WebUI` / `Mobile` calls directly inside test case scripts.

---

## OBJECT REPOSITORY (SHARED — CUSTOMS, NOT PRACTO)

Create **once** under e.g. **`Object Repository/pages/customs_management/`**:

| Test Object (suggested name) | Purpose | Initial locator strategy |
|------------------------------|---------|---------------------------|
| `ddl_GL` | GL dropdown | Replace with real `id`/`name`/`data-testid` from DEV inspect or doc |
| `btn_History` | History CTA | Text or `aria-label` matching Section 10 |
| `btn_Reorder` | Reorder CTA | Same |
| `btn_AddNewRule` | Add New Rule CTA | Same |
| `lbl_PageReady` | Optional anchor for “page loaded” | Stable header or breadcrumb from doc |
| `msg_ErrorOrValidation` | Negative path message container | Section 6 / inspect |

**Do not** duplicate these objects per test case.

**Locator rule:** If **`APPLICATION_CONTEXT.md`** or **Figma** gives canonical selectors, **use them strictly**. If not, use **one** conservative placeholder and a **comment** in OR description: “Replace after DEV inspect — do not ship to CI until updated.”

---

## GLOBAL VARIABLES (EXECUTION PROFILE)

Extend **`default`** (or named profile) with at least:

- **`URL`** — default `'http://dev-cc.aramex.com/customs-management'` unless superseded by product doc.  
- **`GL_VALID`** — non-secret string; value from test data owner / doc Section 3.  
- **`GL_INVALID`** — deterministic invalid or restricted GL for negative path per Section 4.

**Do not** add **`USERNAME` / `PASSWORD`** for this customs smoke **unless** login is in scope per **`APPLICATION_CONTEXT.md`**.

---

## VALIDATION RULES (CONTROLLED FALLBACK)

**Positive (TC_CM_001)**

- After valid GL: **three CTAs** (or Section 10 equivalents) **visible** (and **clickable** if product requires interaction test in a later story).  
- Use **`waitForElementPresent`** then **`waitForElementVisible`**; timeout **30** s.

**Negative (TC_CM_002)**

- Wait for **validation/error** OR stable “blocked” UI state per OR **`msg_ErrorOrValidation`** / disabled primary actions.  
- Assert user is **not** treated as fully successful post-GL if that violates Section 4 (define exact assertion when doc is attached).

**If locator not in doc:** use **single** safe OR placeholder; **do not** stop generation — document replacement in OR description.

---

## SCREENSHOT STRATEGY (MANDATORY)

1. After: **page load**, **GL select**, **each CTA verification milestone**, **negative assertion**, **final success/failure**.  
2. Use **`WebUI.takeScreenshot()`** only (no custom file paths) for Katalon report embedding.  
3. In **every** `catch`: screenshot **before** `KeywordUtil.markFailedAndStop`.  
4. On **success**: final screenshot after last assertion.  
5. Do **not** skip screenshots.

*(JIRA attachment of screenshots is manual unless Katalon-JIRA integration is configured — do not assume.)*

---

## TEST SUITE + EXECUTION

1. Create test suite **`CUSTOMS-LANDING-TEST-SUITE`** (or project naming convention: **`CM-SMOKE-TEST-SUITE`**).  
2. Add **TC_CM_001** and **TC_CM_002** in order.  
3. **Execution:** The automation architect **delivers** suite + scripts; **the operator** runs the suite in **Katalon Studio** on **Chrome** (or via **Katalon CLI** in CI if the project already has it). **Do not** claim unattended execution inside the prompt unless a runnable CLI command and agent are defined in the repo.

---

## OUTPUT (CONTROLLED)

Generate **only**:

1. Test Case script: **TC_CM_001**  
2. Test Case script: **TC_CM_002**  
3. **`Keywords/business/CustomsManagementKeywords.groovy`**  
4. **Object Repository** objects under **`pages/customs_management/`** (shared)  
5. **Profile updates** for `URL`, `GL_VALID`, `GL_INVALID` (no secrets)  
6. **Test suite** **`CUSTOMS-LANDING-TEST-SUITE`** including both cases  

**Do not** output: random CLI one-liners, self-healing logic, or duplicated login/Practo artifacts.

---

## FINAL GOAL

- **Single** reusable customs landing layer  
- **Zero** duplication of GL/CTA logic  
- **Stable** waits, deterministic profile data  
- **Compilation-clean** in Katalon  
- **Traceable** to manual **AC1** positive/negative family (**TC_CM_001** / **TC_CM_002**)

If structure is correct: **preserve** reusable design; only replace OR locators when **DEV** or **`APPLICATION_CONTEXT.md`** supplies them.

---

## MAPPING NOTE (MANUAL ↔ AUTOMATION)

| Manual (example) | Automation (this prompt) |
|------------------|---------------------------|
| AC1 positive | **TC_CM_001** |
| AC1 negative | **TC_CM_002** |
| AC2–AC4 | Future scripts **TC_CM_003+** — **out of scope** unless explicitly added to this prompt in a later revision |

---

*End of Customs Management automation prompt.*
