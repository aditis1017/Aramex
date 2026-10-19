# QA Document Index

Update this file when Jira projects, Confluence spaces, or feature mappings change.
Agent-2 reads this first before searching Atlassian.

---

## Atlassian Site

| Field | Value |
|-------|-------|
| Site URL | `https://YOUR-COMPANY.atlassian.net` |
| Cloud ID | _(leave blank — use `getAccessibleAtlassianResources` if hostname fails)_ |

---

## Jira

| Field | Value |
|-------|-------|
| Project Key | `PROJ` |
| Board URL | `https://YOUR-COMPANY.atlassian.net/jira/software/projects/PROJ/boards/1` |
| Test project (if separate) | `QA` |

### Common JQL

```
# Open stories in project
project = PROJ AND type = Story AND status != Done ORDER BY priority DESC

# Story by key
key = PROJ-123

# Epic and its stories
"Epic Link" = PROJ-100

# Stories updated this sprint
project = PROJ AND sprint in openSprints() AND type = Story
```

---

## Confluence

| Field | Value |
|-------|-------|
| QA / Test space key | `QA` |
| Product / PRD space key | `PROD` |
| QA space home | `https://YOUR-COMPANY.atlassian.net/wiki/spaces/QA` |
| Test strategy page ID | `REPLACE_WITH_PAGE_ID` |
| API docs page ID | `REPLACE_WITH_PAGE_ID` |

---

## Feature → Document Mapping

| Feature | Jira Epic / Story | Confluence Page ID | Notes |
|---------|-------------------|--------------------|-------|
| Login | PROJ-100 / AUTH-101 | `111111111` | Login spec §2.x |
| Registration | PROJ-100 / AUTH-102 | `111111112` | Registration flow |
| Checkout | PROJ-200 / CHECKOUT-201 | `222222221` | Payment integration |
| _Add rows as needed_ | | | |

---

## Search Hints

When the user gives a **feature name** (not a Jira key), search in this order:

1. Match row in **Feature → Document Mapping** above
2. Rovo `search` with: `"<feature name>" project PROJ`
3. Confluence CQL (if needed): `text ~ "<feature name>" AND space = QA`

When the user gives a **Jira key** (e.g. `PROJ-123`):

1. `getJiraIssue` for that key
2. Read description, acceptance criteria, linked issues, remote links
3. `search` for the story title + epic name to find Confluence pages
4. `getConfluencePage` for any page IDs from mapping or search results
