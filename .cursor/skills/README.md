# Project Agent Skills (Cursor)

Cursor loads skills from **`.cursor/skills/<skill-name>/SKILL.md`**. Each file must start with YAML frontmatter:

```yaml
---
name: skill-name-kebab-case
description: >-
  One paragraph: what it does and when to invoke (include slash command and triggers).
---
```

## How to use in Cursor

1. **Chat — attach skill:** Type `@` and pick the skill (e.g. `cci-e2e-qa-pipeline`, `cci-automation-from-story`) or add the folder under **Cursor Settings → Rules / Skills** if your build supports project skills.
2. **Slash-style prompt:** Include the command in the message, e.g. `/cci-e2e-qa-pipeline CCIPH-385`.
3. **Prerequisites:** Atlassian MCP connected for Jira skills; Katalon project open for automation output.

## CCI skills (recommended)

| Skill name | Invoke | Input |
|------------|--------|--------|
| **cci-e2e-qa-pipeline** | `/cci-e2e-qa-pipeline CCIPH-###` | Full pipeline: xlsx + sub-tasks → automation → RTM (`environment dev` default, `environment staging` optional) |
| **cci-automation-from-story** | `/cci-automation-from-story CCIPH-###` | Katalon only (manual TCs must exist) |
| qa-manual-test-case | `/qa-manual-test-case CCIPH-###` | Linked Jira **Test Case** issues (not sub-tasks) |
| co-pilot-automation-skill | `/co-pilot-automation-skill CCIPH-###` | Katalon from linked Test Cases (legacy path) |
| **github-aramex-push** | `/github-aramex-push` + commit message | Push the current branch to `github` (`aditis1017/Aramex`) and start GitHub Actions |
| **github-aramex-ci-self-heal** | `/github-aramex-ci-self-heal` + commit message | Stage all, pull, resolve conflicts, push, then re-execute a failed CI run up to 3 times |

Manual sub-tasks + xlsx (no Cursor skill folder — lives under cci-skills):

- `.cursor/cci-skills/cci-manual-tc-subtask-xlsx/SKILL.md` — used by **cci-e2e-qa-pipeline** Phase 1

## Supporting assets

| Path | Purpose |
|------|---------|
| `.cursor/cci-skills/Sample File Reference/` | Test Case.xlsx, CCI-RTM.xlsx templates |
| `.cursor/cci-skills/FE-ApplicationCode/` | Locators / UI reference |
| `.cursor/cci-skills/BE-ApplicationCode/` | API / rules reference |
| `.cursor/rules/*.mdc` | Auto-attach rules when phrasing matches (e2e, automation, etc.) |

## Rules that trigger CCI workflows

| Rule | When |
|------|------|
| [cci-e2e-qa-pipeline.mdc](../rules/cci-e2e-qa-pipeline.mdc) | e2e / full pipeline / `/cci-e2e-qa-pipeline` |
| [cci-automation-from-story.mdc](../rules/cci-automation-from-story.mdc) | automate story / Katalon generation |
| [cci-e2e-pipeline.mdc](../rules/cci-e2e-pipeline.mdc) | alias for e2e-pipeline wording |

## All skills in this folder

| Folder | `name` in frontmatter |
|--------|------------------------|
| cci-e2e-qa-pipeline | cci-e2e-qa-pipeline |
| cci-automation-from-story | cci-automation-from-story |
| qa-manual-test-case | qa-manual-test-case |
| co-pilot-automation-skill | co-pilot-automation-skill |
| qa-jira-defect-logger-subtask-assignee | qa-jira-defect-logger-subtask-assignee |
| qa-jira-suite-result-updater | qa-jira-suite-result-updater |
| qa-knowledge-retriever | qa-knowledge-retriever |
| qa-test-case-writer | qa-test-case-writer |
| github-aramex-push | github-aramex-push |
| github-aramex-ci-self-heal | github-aramex-ci-self-heal |
