# Sets up project-level Atlassian MCP auth for this workspace.
# Run from this folder: .\.cursor\setup-atlassian-mcp.ps1
#
# Prerequisites:
# - A Rovo MCP scoped API token (ATATT...) from id.atlassian.com
# - Your org admin must enable API token auth for Rovo MCP Server
# - Your Atlassian account email

param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Parameter(Mandatory = $true)]
    [string]$ApiToken
)

$ErrorActionPreference = "Stop"

$credentials = "${Email}:${ApiToken}"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($credentials)
$base64 = [Convert]::ToBase64String($bytes)

$mcpConfig = @{
    mcpServers = @{
        atlassian = @{
            url = "https://mcp.atlassian.com/v1/mcp"
            headers = @{
                Authorization = "Basic $base64"
            }
        }
    }
} | ConvertTo-Json -Depth 5

$outPath = Join-Path $PSScriptRoot "mcp.json"
$mcpConfig | Set-Content -Path $outPath -Encoding UTF8

Write-Host "Created $outPath"
Write-Host "Reload MCP in Cursor: Settings > MCP > Restart, or restart Cursor."
