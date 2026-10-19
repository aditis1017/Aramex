# QA workspace (CCM / Custom Charges)

Single place to orient **manual testing**, **Jira**, **RTM**, **defects**, and **Katalon automation** against application source and requirements.

## Repository map

```
Corp.microservice.ccm-automation-AI/
├── FE-ApplicationCode/          # Next.js UI (reference)
├── BE-ApplicationCode/          # Java microservices (reference)
├── qa/                          # ← You are here
├── Test Cases/                  # Katalon test cases (by feature / CCIPH-*)
├── Test Suites/                 # Katalon suites
├── Object Repository/           # Katalon locators
├── Keywords/                    # Katalon custom keywords
├── Scripts/                     # Katalon script bodies
├── Data Files/                  # Katalon test data
└── .cursor/skills/              # QA agent workflows (Jira TC, automation, defects, RTM)
```

## Folders under `qa/`

| Folder | Purpose |
|--------|---------|
| `context/` | Pointers to product/architecture context for agents |
| `traceability/` | RTM, module ↔ code map, source refresh notes |
| `manual-test-cases/templates/` | CSV/XLSX templates (e.g. SampleTemplate) |
| `manual-test-cases/exports/jira/` | CSV exports destined for Jira import or review |
| `manual-test-cases/exports/xlsx/` | Excel functional test case workbooks |
| `defects/` | Defect templates and export snapshots |
| `reporting/` | SIT / release report templates |
| `automation/` | Index and notes for Katalon structure |

## Agent skills (common flows)

| Goal | Skill path |
|------|------------|
| Manual TCs → Jira | `.cursor/skills/qa-manual-test-case/` |
| Manual TCs → Jira **Sub-tasks** + **Test Case.xlsx** | `.cursor/cci-skills/cci-manual-tc-subtask-xlsx/` |
| Manual TCs → CSV (SampleTemplate) | `.cursor/skills/qa-test-case-writer/` |
| Requirements from Jira/Confluence | `.cursor/skills/qa-knowledge-retriever/` |
| Katalon scripts from story | `.cursor/skills/co-pilot-automation-skill/` |
| Katalon from story + **cci-skills FE/BE** locators | `.cursor/skills/cci-automation-from-story/` |
| **E2E:** xlsx + sub-tasks → automation → CCI-RTM | `.cursor/skills/cci-e2e-qa-pipeline/` |
| **All Cursor skills** | `.cursor/skills/README.md` |
| Suite results → Jira TC status | `.cursor/skills/qa-jira-suite-result-updater/` |
| Failures → Jira bugs | `.cursor/skills/qa-jira-defect-logger/` |
| Failures → Jira bugs (assignee from Backend/FE **sub-task** reporter) | `.cursor/skills/qa-jira-defect-logger-subtask-assignee/` |
| Stakeholder e2e pipeline | `.cursor/skills/cci-e2e-pipeline/` (when present) |

## Traceability

- **Module map:** [`traceability/module-map.yaml`](traceability/module-map.yaml) — links Katalon folders, UI routes, and backend services.
- **RTM workbook:** [`traceability/rtm/RTM.xlsx`](traceability/rtm/RTM.xlsx)
- **Application context:** [`../Application Context.md`](../Application%20Context.md) (CCPS overview); add Confluence links in `context/`.

## Conventions

- Jira keys: **CCIPH-** user stories and test cases.
- Name manual exports: `<Feature>_CCIPH-<story>_<date>.csv` or `.xlsx`.
- Keep generated Katalon output out of git (`Reports/`, `bin/`, etc. — see root `.gitignore`).
- Application source under `FE-ApplicationCode/` and `BE-ApplicationCode/` is for **analysis and traceability**; default git pushes include Katalon only unless you explicitly include source trees.
