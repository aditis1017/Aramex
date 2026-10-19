# Jira updates after automation

After Katalon assets are generated for a Jira test artifact, update traceability.

## Labels (per automated TC)

On each **Test Case** or **manual-test Sub-task** issue:

| Label | When |
|-------|------|
| `Automated` | **cci-e2e-qa-pipeline** (preferred team label) |
| `automation` | Standalone **cci-automation-from-story** (optional; may coexist) |
| `katalon` | Always with automation |
| `TC_<id>` | If not already present |

Use `editJiraIssue` to merge labels (do not remove existing `manual-test`, `tc-type-*`).

## Comment (optional but recommended)

```markdown
## Katalon automation generated

| Field | Value |
|-------|-------|
| Story | CCIPH-### |
| Katalon TC | Test Cases/<MODULE>/<STORY>/TC_....tc |
| Script | Scripts/<MODULE>/<STORY>/....groovy |
| Suite | Test Suites/CCI_<ABBR>_CCIPH-###_SUITE.ts |

*Generated via cci-automation-from-story*
```

## Test Case.xlsx mirror

If export exists at `qa/manual-test-cases/exports/xlsx/*_<STORY>_*.csv`:

- Set `Automation (Y/N)` = `Y` for automated rows
- Set `Automation Execution Status` = `Not Run`
- Set `Automation Remarks` = Katalon script file name

Re-run [export-test-case-xlsx.ps1](../../../qa/manual-test-cases/scripts/export-test-case-xlsx.ps1) if Excel workbook must be refreshed.

## Suite result handoff

After execution, use [qa-jira-suite-result-updater](../../skills/qa-jira-suite-result-updater/SKILL.md) with the same `STORY_KEY`.
