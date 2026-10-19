# Atlassian MCP (Jira write access, no browser login)

CCI QA skills use the **Atlassian MCP** server to create Jira issues, sub-tasks, comments, and transitions. OAuth in Cursor can require repeated approval; **API token auth** in project `mcp.json` avoids that.

## 1. Create a Rovo MCP API token

1. Sign in at [Atlassian API tokens](https://id.atlassian.com/manage-profile/security/api-tokens).
2. Create a token scoped for **Rovo MCP** (token value starts with `ATATT...`).
3. If your org blocks this, ask an Atlassian admin to enable **API token authentication for Rovo MCP Server**.

Use the **email** of the Atlassian account that has CCIPH create/edit permission on `aramex.atlassian.net`.

## 2. Write `mcp.json` (local only)

From the **repository root** in PowerShell:

```powershell
$env:ATLASSIAN_EMAIL = "your.email@company.com"
$env:ATLASSIAN_API_TOKEN = "ATATT..."   # paste once; do not commit or share in chat
.\.cursor\setup-atlassian-mcp.ps1
```

Or run `.\.cursor\setup-atlassian-mcp.ps1` and enter email + token at the prompts (token is hidden).

Template: [mcp.json.example](./mcp.json.example). Output file: `.cursor/mcp.json` (listed in `.cursor/.gitignore`).

## 3. Reload MCP in Cursor

**Settings → MCP** → restart the **atlassian** server (or restart Cursor).

Open this workspace so Cursor loads **project** `.cursor/mcp.json`, not only the global MCP list.

## 4. Verify

In Agent chat, ask to run `getAccessibleAtlassianResources`. You should see `aramex.atlassian.net` with Jira `read-write`.

Then run a small write (e.g. add a comment on a test issue you own) before using the full e2e pipeline.

## Security

- **Never** commit `.cursor/mcp.json` or paste tokens into Jira tickets, Confluence, or git.
- If a token was ever committed, **revoke it** in Atlassian and create a new one.
- If the file was tracked before `.gitignore`, run `git rm --cached .cursor/mcp.json` and commit that change (keeps your local file).

## Cursor still asks to approve MCP tools?

That is separate from Jira auth: Cursor may prompt before **executeWrite** / **createJiraIssue**. Approve once per session or adjust MCP auto-run settings in Cursor. Token auth fixes **Atlassian login**, not every Cursor safety prompt.
