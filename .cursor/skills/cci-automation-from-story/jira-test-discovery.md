# Jira test case discovery (dual upstream)

**cci-automation-from-story** automates from Jira specs created by either upstream manual skill.

## Upstream A — `qa-manual-test-case`

| Attribute | Value |
|-----------|--------|
| Issue type | **Test Case** (standalone) |
| Link to story | `createIssueLink` — `issuelinks` on story |
| Summary | `TC_GS_001: …` or `TC-001: …` |
| Description sections | `Test Case Details`, `Preconditions`, `Test Steps`, `Expected Result` |

Discovery:

1. `getJiraIssue(STORY)` → `issuelinks`
2. Accept link types: `Relates`, `Tests`, `Test`, `Relates to`, `is tested by`, …
3. Filter issue types: `Test Case`, `Test`, `Xray Test`
4. Fallback JQL: `project = CCIPH AND issue in linkedIssues(STORY) AND (summary ~ "TC_" OR summary ~ "TC-")`

## Upstream B — `cci-manual-tc-subtask-xlsx`

| Attribute | Value |
|-----------|--------|
| Issue type | **Sub-task** |
| Parent | `STORY_KEY` |
| Labels | `manual-test`, `TC_<ID>`, `tc-type-*` |
| Summary | `[TC_GS_FUNC_001] Title` |
| Description | [jira-subtask-template.md](../cci-manual-tc-subtask-xlsx/jira-subtask-template.md) |

Discovery:

```text
parent = <STORY_KEY> AND issuetype in subTaskIssueTypes() AND labels = manual-test ORDER BY created ASC
```

Also accept sub-tasks whose summary matches `^\[TC_`.

**Optional xlsx filter:** If `qa/manual-test-cases/exports/xlsx/*_<STORY>_*.csv` exists, prefer rows where `Automation (Y/N)` = `Y`; if column empty, automate all **Functional** + **Regression** types unless user says `all` or `required only`.

## Selection modes

| Mode | Behavior |
|------|----------|
| Default `ALL` | All discovered TCs from A + B (dedupe by TC_ID) |
| `linked-only` | Upstream A only |
| `subtasks-only` | Upstream B only |
| `keys CCIPH-1, CCIPH-2` | Subset |
| `prefix TC_APM_` | Summary filter |
| `required` | xlsx `Automation (Y/N)=Y` or label `automation-candidate` |

## Parsing sub-task descriptions

Map to internal spec:

| Field | Source |
|-------|--------|
| `tcId` | Summary `[TC_…]` or label |
| `testSteps` | `### Test Steps` numbered list |
| `expectedResult` | `### Expected Result (overall)` — single sentence |
| `preconditions` | `### Preconditions` |
| `testCaseType` | `Test Case Type` row in description table |
| `screenName` | `Screen Name` in description table |

## Zero candidates

Report and suggest:

1. `/qa-manual-test-case <STORY>` — linked Test Cases, or  
2. `/cci-manual-tc-subtask-xlsx <STORY>` — sub-tasks + xlsx  

Do not invent steps from story AC unless user says `scaffold from AC`.
