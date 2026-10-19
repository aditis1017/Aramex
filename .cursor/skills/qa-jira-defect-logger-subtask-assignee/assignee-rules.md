# Developer assignee rules (CCIPH sub-tasks)

Used by **qa-jira-defect-logger-subtask-assignee** when setting the Bug **assignee** after automation failures.

## Source of assignee (not the User Story)

| Do not use | Use instead |
|------------|-------------|
| Parent **User Story** reporter | **Sub-task** under that story (Jira `parent = <STORY_KEY>`) |
| Linked issues (other stories, test cases) | Only **child Sub-tasks** of the parent story |

The sub-task represents the **development work** (Backend or Frontend) for that story.

## Sub-task discovery

1. `getJiraIssue` on `STORY_KEY` with subtasks / child issue keys if returned.
2. `searchJiraIssuesUsingJql`:
   ```text
   parent = <STORY_KEY> AND issuetype in subTaskIssueTypes()
   ```
   If your site uses a named type instead:
   ```text
   parent = <STORY_KEY> AND issuetype = "Sub-task"
   ```
3. For each sub-task key, `getJiraIssue` with fields: `summary`, `labels`, `reporter`, `assignee`, `status`, `issuetype`.

## Layer labels on sub-tasks (required)

Each dev sub-task should carry a label identifying the workstream:

| Layer | Acceptable labels (case-insensitive match) |
|-------|---------------------------------------------|
| **Backend** | `Backend`, `Back-end`, `BE`, `API`, `backend` |
| **Frontend** | `Frontend`, `Front-end`, `FE`, `UI`, `Web-UI`, `frontend` |

If multiple sub-tasks match the same layer, prefer:

1. Sub-task in **In Progress** or **Done** (most recently updated)
2. Sub-task whose **summary** contains `Backend` / `Frontend` / `FE` / `BE`
3. First match in JQL order

If **no** sub-task has layer labels, stop and ask the user which sub-task key to use — do not fall back to story reporter without confirmation.

## Which layer for the new Bug?

Determine **defect layer** before picking a sub-task:

| Priority | Signal |
|----------|--------|
| 1 | User override: `layer backend` or `layer frontend` in chat |
| 2 | User override: `assign from sub-task CCIPH-1234` |
| 3 | Labels on the **failed automation / test case** Jira issue (if linked): `backend` / `frontend` / `ui` |
| 4 | Failure evidence (Katalon): WebUI + Object Repository path → **Frontend**; HTTP/WS keywords, API URL in logs → **Backend** |
| 5 | Default for CCI Web UI automation failures | **Frontend** |

Add matching labels on the new Bug: `backend` or `frontend` (plus existing `automation`, `katalon`, `TC_*`).

## Who becomes Bug assignee?

Per product request, use the sub-task **Reporter** as the Bug assignee (treated as the developer contact for that workstream).

```text
assigneeAccountId = subTask.fields.reporter.accountId
```

**Fallback** (only if reporter is missing or is clearly a bot/service account):

```text
assigneeAccountId = subTask.fields.assignee.accountId
```

Never use parent story reporter unless user explicitly overrides after a failed sub-task lookup.

## Jira API notes

- On `createJiraIssue`, set assignee when the project allows it:
  ```json
  "assignee": { "id": "<accountId>" }
  ```
- If create rejects assignee, create the bug then `editJiraIssue` with the same assignee field.
- Use `atlassianUserInfo` only to validate the current user — not as default assignee.

## Delivery summary fields

Always report in Phase 5:

- Parent story key
- Chosen layer (`Backend` | `Frontend`)
- Sub-task key used for assignee
- Sub-task summary + labels
- Assignee display name (from sub-task reporter or fallback assignee)
