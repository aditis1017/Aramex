#Requires -Version 5.1
<#
.SYNOPSIS
  Fills CCI-RTM.xlsx from a UTF-8 CSV matching the RTM template headers.
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
    $TemplateXlsx = Join-Path $repoRoot ".cursor\cci-skills\Sample File Reference\CCI-RTM.xlsx"
}
if (-not (Test-Path $TemplateXlsx)) {
    $TemplateXlsx = Join-Path $repoRoot "qa\traceability\rtm\CCI-RTM.xlsx"
}
if (-not (Test-Path $TemplateXlsx)) {
    throw "RTM template not found: $TemplateXlsx"
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
            $ws.Cells.Item($r, $c) = [string]$prop.Value
        }
        $r++
    }

    $wb.Save()
    $wb.Close()
    $excel.Quit()
    Write-Host "Wrote $($rows.Count) RTM row(s) to $OutputXlsx"
}
catch {
    Write-Warning "Excel COM export failed: $($_.Exception.Message). CSV at $CsvPath"
    exit 1
}
