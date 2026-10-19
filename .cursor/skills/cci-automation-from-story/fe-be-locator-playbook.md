# FE / BE locator & method playbook

Application reference roots (cci-skills copies):

| Layer | Path |
|-------|------|
| Frontend | `.cursor/cci-skills/FE-ApplicationCode/` |
| Backend | `.cursor/cci-skills/BE-ApplicationCode/services/` |
| Module map | `qa/traceability/module-map.yaml` |
| FE route index | `.cursor/cci-skills/FE-ApplicationCode/docs/qa/README.md` |

## Step 1 — Map MODULE_ROOT → FE route

From story title (text before `|`) and [module-map.yaml](../../../qa/traceability/module-map.yaml), resolve:

- Katalon folder name (e.g. `Global Settings`)
- FE path (e.g. `app/customs-management/global-settings`)
- Component folder (e.g. `app/components/GlobalSettings/`)

## Step 2 — FE locator discovery (before new OR)

Search in the mapped FE paths:

| Signal | Search pattern | OR preference |
|--------|----------------|---------------|
| Accessible name | `aria-label=` | CSS `[aria-label='...']` or XPath |
| Form fields | `label`, `placeholder`, `name=` | id/name if stable |
| Buttons | button text in TSX, `type="submit"` | XPath normalize-space text |
| Sections | `aria-labelledby`, heading `id=` | section-scoped XPath |
| i18n keys | `t('...')` | use English string from messages if grep finds key |

**Grep examples (agent):**

- `aria-label` in `app/components/<Module>/`
- `entity-settings-col-` prefix (entity tables)
- Route page: `app/customs-management/<area>/page.tsx`

Match existing OR style in repo, e.g.:

```text
Object Repository/Entity Management-AR/.../select[@aria-label='entity-settings-col-...']
```

## Step 3 — Reuse Katalon Keywords (methods)

| Module | Typical keyword class |
|--------|------------------------|
| AP / AR | `Keywords/ap_ar_management/BusinessActions.groovy` |
| Global Settings | `Keywords/globalsettings/BusinessActions.groovy` |
| Entity | `Keywords/entitymanagement/BusinessActions.groovy` |
| Custom Search | `Keywords/customsearch_360viewscreen/BusinessActions.groovy` |
| Upload | `Keywords/Upload_Estimate_Actuals/BusinessActions.groovy` |
| Login (suite only) | `Keywords/customsmanagement/MicrosoftLoginKeywords.groovy` |

Add **new** methods to the existing `BusinessActions` for the module when the action is reusable across TCs; keep test scripts thin.

## Step 4 — BE reference (expectations & integration TCs)

Use BE only to strengthen **expected results** and integration scenarios — not DOM locators.

| Service folder | Use for |
|----------------|---------|
| `services/calculator` | Charge types, calculation rules, error enums |
| `services/persistence` | Entity names, validation messages, REST paths |
| `services/file-orchestration` | Upload processing states |
| `services/shipment` | Shipment ingest, B2B |
| `services/global-settings` | Rule configuration behavior |

Grep Java for `errorMessage`, `Exception`, validation annotations when the UI should show a specific message.

## Step 5 — Test Case Type → automation depth

From [cci-manual-tc-subtask-xlsx test-case-xlsx-schema](../cci-manual-tc-subtask-xlsx/test-case-xlsx-schema.md):

| Test Case Type | Automation focus |
|----------------|------------------|
| Functional Testing | Full Web UI script |
| Regression Testing | Reuse stable OR; prefer existing keywords |
| Integration Testing | UI + optional API verify keyword if project has one |
| Security Testing | RBAC — assert hidden/disabled controls via FE rbac paths |
| Accessibility Testing | aria-label presence / focusable controls (lightweight checks) |
| Usability / Performance | Usually **skip** full automation unless user requests; mark N in xlsx |

## Accuracy checklist (per TC)

- [ ] Every step element exists in FE grep or existing OR
- [ ] No invented button/field labels
- [ ] Expected Result maps to one `WebUI.verify*` or Keyword assert
- [ ] Negative TC uses UI-visible failure (not HTTP-only)
