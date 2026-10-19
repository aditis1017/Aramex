# Test environments (Dev / Staging)

Used by **cci-e2e-qa-pipeline** (Phases 0–2) and **cci-automation-from-story**. Align with Katalon `Profiles/default.glbl` globals.

## Selection

| Chat override | `ENVIRONMENT` | When |
|---------------|---------------|------|
| *(none)* | **`dev`** | Default for manual preconditions, xlsx notes, and automation navigation |
| `environment staging` / `env staging` / `on staging` | **`staging`** | Staging URLs and stage profile globals |
| `environment dev` / `env dev` | **`dev`** | Explicit dev |
| `url https://...` | custom | Use exact base; set `ENVIRONMENT=custom` in delivery report |

Resolve **once** in Phase 0; pass the same `ENVIRONMENT` and URLs to Phase 1 preconditions and Phase 2 automation.

## Base URLs

| Environment | Customs Management app root | Login / entry (typical) |
|-------------|----------------------------|-------------------------|
| **dev** | `https://dev-cc.aramex.com/customs-management` | `http://dev-cc.aramex.com/customs` or app root above |
| **staging** | `https://stage-cc.aramex.com/customs-management` | `https://stage-cc.aramex.com/customs` |

Use **HTTPS** for module deep links when both http/https exist in profile; match existing Keywords for the target env.

## Module deep links (`{base}/customs-management/...`)

Replace host with `dev-cc` or `stage-cc` per environment.

| MODULE_ROOT (Katalon) | Path segment | Dev example |
|----------------------|--------------|-------------|
| AP Management | `ap-management` | `https://dev-cc.aramex.com/customs-management/ap-management` |
| AR Management | `ar-management` | `https://dev-cc.aramex.com/customs-management/ar-management` |
| Global Settings | `global-settings` | `https://dev-cc.aramex.com/customs-management/global-settings` |
| Entity Management-AP / AR | `entity-settings` | `https://dev-cc.aramex.com/customs-management/entity-settings` |
| Roles&Permission | `roles-permissions` | `https://dev-cc.aramex.com/customs-management/roles-permissions` |
| B2B-Shipment | `b2b-keywords-manager` | `https://dev-cc.aramex.com/customs-management/b2b-keywords-manager` |
| CustomSearch_360ViewScreen | `customs-search` | `https://dev-cc.aramex.com/customs-management/customs-search` |
| Upload Charges / estimates | `customs/upload-charges` | `https://dev-cc.aramex.com/customs/upload-charges` |

**Staging:** same path segments on `https://stage-cc.aramex.com` (e.g. `https://stage-cc.aramex.com/customs-management/global-settings`).

## Katalon profile globals

When generating or documenting automation:

| Environment | Suggested profile globals to cite in suite description |
|-------------|--------------------------------------------------------|
| dev | `CUSTOMS_APP_URL`, `AP_MANAGEMENT_URL`, `AR_MANAGEMENT_URL`, `GLOBAL_SETTINGS_URL`, etc. (dev-cc values in `Profiles/default.glbl`) |
| staging | `Stage_*` or stage-cc initValues in the same profile file |

Do **not** commit secrets (`MS_LOGIN_PASSWORD`, IMAP passwords). Preconditions should name **role** and **environment URL**, not credentials.

## Manual test case preconditions (Phase 1)

Every generated precondition must include:

```text
Environment: <dev|staging>
Application URL: <module deep link or app root from table above>
Role: OPS | ADMIN | … (from story)
```

## Automation (Phase 2)

- Pass `environment <dev|staging>` into **cci-automation-from-story** (or equivalent `url` for module entry).
- Prefer `WebUI.openBrowser` / Keywords that read **GlobalVariable** for the active profile when scripts already do; for new scripts, use module URL from this doc for the resolved environment.
- Suite description: `Environment: dev | staging | CCIPH-###`

## Delivery report

Always state:

```text
ENVIRONMENT: dev | staging | custom
BASE_URL: <resolved>
MODULE_URL: <if applicable>
```
