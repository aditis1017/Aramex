#Requires -Version 5.1
<#
.SYNOPSIS
  Fills Test Case.xlsx from a UTF-8 CSV that matches the canonical column headers.

.PARAMETER CsvPath
  CSV with header row matching Test Case.xlsx (see .cursor/cci-skills/cci-manual-tc-subtask-xlsx/test-case-xlsx-schema.md).

.PARAMETER OutputXlsx
  Destination .xlsx path (created from template if missing).

.PARAMETER TemplateXlsx
  Default: .cursor/cci-skills/Sample File Reference/Test Case.xlsx
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $CsvPath,
    [Parameter(Mandatory = $true)]
    [string] $OutputXlsx,
    [string] $TemplateXlsx = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) -Parent
if (-not $TemplateXlsx) {
    $TemplateXlsx = Join-Path $repoRoot ".cursor\cci-skills\Sample File Reference\Test Case.xlsx"
}
if (-not (Test-Path $TemplateXlsx)) {
    $TemplateXlsx = Join-Path $repoRoot "qa\manual-test-cases\templates\Test Case.xlsx"
}
if (-not (Test-Path $TemplateXlsx)) {
    throw "Template not found: $TemplateXlsx"
}
if (-not (Test-Path $CsvPath)) {
    throw "CSV not found: $CsvPath"
}

$outDir = Split-Path $OutputXlsx -Parent
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}
Copy-Item -LiteralPath $TemplateXlsx -Destination $OutputXlsx -Force

$rows = Import-Csv -LiteralPath $CsvPath -Encoding UTF8
if ($rows.Count -eq 0) {
    Write-Warning "CSV has no data rows."
    exit 0
}

try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $wb = $excel.Workbooks.Open($OutputXlsx)
    $ws = $wb.Worksheets.Item(1)

    $colCount = $ws.UsedRange.Columns.Count
    $headerMap = @{}
    for ($c = 1; $c -le $colCount; $c++) {
        $h = [string]$ws.Cells.Item(1, $c).Text
        if ($h) { $headerMap[$h.Trim()] = $c }
    }

    $r = 2
    foreach ($row in $rows) {
        foreach ($prop in $row.PSObject.Properties) {
            $name = $prop.Name
            if (-not $headerMap.ContainsKey($name)) { continue }
            $c = $headerMap[$name]
            $val = [string]$prop.Value
            $ws.Cells.Item($r, $c) = $val
        }
        $r++
    }

    $wb.Save()
    $wb.Close()
    $excel.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($ws) | Out-Null
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wb) | Out-Null
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
    Write-Host "Wrote $($rows.Count) row(s) to $OutputXlsx"
}
catch {
    Write-Warning "Excel COM export failed: $($_.Exception.Message). CSV remains at $CsvPath — paste into template manually."
    exit 1
}
