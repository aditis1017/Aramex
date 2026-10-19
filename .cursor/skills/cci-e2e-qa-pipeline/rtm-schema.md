# CCI-RTM.xlsx — traceability matrix schema

**Template (do not change headers in row 1):**  
`.cursor/cci-skills/Sample File Reference/CCI-RTM.xlsx`

## Columns

| # | Column | Source when filling |
|---|--------|---------------------|
| 1 | JIRA Epic ID | Parent epic of `STORY_KEY` from `getJiraIssue` (`fields.parent` or Epic Link custom field); blank if none |
| 2 | JIRA User Story ID | `CCIPH-###` |
| 3 | JIRA User Story Title | Story summary |
| 4 | JIRA Test Case ID | QA sub-task key (functional TC) |
| 5 | JIRA Test Case Title | Sub-task summary (without `[TC_ID]` prefix optional) |
| 6 | Automation Status | See [status values](#automation-status-values) |
| 7 | Remark | Katalon script path, suite name, or `Manual only — Automation (Y/N)=N` |

Trailing empty columns in the template may exist — leave blank.

## Automation Status values

| Value | When |
|-------|------|
| `Not Applicable` | `Automation (Y/N)=N` on Test Case.xlsx row |
| `Planned` | `Automation (Y/N)=Y` but scripts not generated yet (optional interim) |
| `Automated` | Katalon `.tc` + script created; Jira label `Automated` applied |
| `Not Run` | After automation, before first suite execution (optional) |
| `Pass` / `Fail` | After `qa-jira-suite-result-updater` (out of scope for this pipeline unless user runs updater) |

Default after **cci-e2e-qa-pipeline** Phase 4: **`Automated`** for scripted TCs; **`Not Applicable`** for others.

## Export path

```text
qa/traceability/rtm/CCI-RTM_<STORY_KEY>_<YYYY-MM-DD>.xlsx
qa/traceability/rtm/CCI-RTM_<STORY_KEY>_<YYYY-MM-DD>.csv
```

## Populate workbook

1. Copy template to export path.
2. Write CSV with exact header row matching template.
3. Run `qa/manual-test-cases/scripts/export-cci-rtm-xlsx.ps1` (same pattern as Test Case export).

One **RTM row per QA functional sub-task** created in Phase 1.
