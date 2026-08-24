# Generate Katalon test cases and scripts for CCIPH-21 (Custom Search)
[CmdletBinding()]
param(
    [string]$RepoRoot = ''
)

$ErrorActionPreference = 'Stop'
if (-not $RepoRoot) { $RepoRoot = (Resolve-Path (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..\\..')).Path }

$SourceCsv = Join-Path $RepoRoot 'CustomSearch_360ViewScreen.csv'
$DataCsvPath = Join-Path $RepoRoot 'Data Files\CustomSearch_CCIPH21_Data.csv'
$DataDatPath = Join-Path $RepoRoot 'Data Files\CustomSearch_CCIPH21_Data.dat'
$DataId = 'Data Files/CustomSearch_CCIPH21_Data'
$TcRoot = Join-Path $RepoRoot 'Test Cases\CustomSearch_360ViewScreen\CCIPH-21'
$ScriptRoot = Join-Path $RepoRoot 'Scripts\CustomSearch_360ViewScreen\CCIPH-21'

$DataColumns = @(
    'ID','charge_Group','charge_Code','charge_Status','date_Type','start_Date','end_Date',
    'search_Criteria','search_Value','tracking_Numbers','invalid_Tracking_Numbers',
    'invalid_Search_Value','favorite_Label','favorite_Label_New','entity_Filter'
)

function Get-TrailingIdNumber {
    param([string]$TestId)
    if ($TestId -match '_(\d+)$') { return [int]$Matches[1] }
    throw "Cannot parse numeric suffix from ID: $TestId"
}

function Get-ScriptFileName {
    param([string]$TestId)
    if ($TestId -match '^TC_US_FUNC_(\d+)$') {
        return ('Script2121002{0:D4}.groovy' -f [int]$Matches[1])
    }
    if ($TestId -match '^TC_Detailed_(\d+)$') {
        return ('Script2121003{0:D4}.groovy' -f [int]$Matches[1])
    }
    $n = Get-TrailingIdNumber -TestId $TestId
    return ('Script2121009{0:D4}.groovy' -f $n)
}

function Get-TestCaseGuid {
    param([string]$TestId)
    $slug = ($TestId -replace '[^A-Za-z0-9]', '-').ToLower()
    return "cs-cciph21-$slug-0001"
}

function Get-TestDataLinkId {
    param([string]$TestId)
    return ('cs21-link-{0}' -f ($TestId -replace '[^A-Za-z0-9]', '-').ToLower())
}

function Get-VariableId {
    param([string]$Name)
    return ('cs21-var-{0}' -f ($Name -replace '_', '-'))
}

$KeywordMap = @{
    'TC_US_FUNC_001' = 'cs.verifySearchWithoutFiltersShowsNoData()'
    'TC_US_FUNC_002' = 'cs.verifySearchWithSingleFilter(charge_Group, charge_Code)'
    'TC_US_FUNC_003' = 'cs.verifySearchWithMultipleFilters(charge_Group, charge_Code, charge_Status)'
    'TC_US_FUNC_004' = 'cs.verifySearchWithAllFilters()'
    'TC_US_FUNC_005' = 'cs.verifySearchWithDateRangeOnly(date_Type, start_Date, end_Date)'
    'TC_US_FUNC_006' = 'cs.verifySearchWithMultipleTrackingNumbers(tracking_Numbers)'
    'TC_US_FUNC_007' = 'cs.verifyInvalidDateRangeError(start_Date, end_Date)'
    'TC_US_FUNC_008' = 'cs.verifyInvalidTrackingNumbersError(invalid_Tracking_Numbers)'
    'TC_US_FUNC_010' = 'cs.verifyCombinedFilterSearch()'
    'TC_US_FUNC_011' = 'cs.verifyPaginationOnSearchResults()'
    'TC_US_FUNC_012' = 'cs.verifyHideAndShowFilters()'
    'TC_US_FUNC_013' = 'cs.verifyFilterLabelsAfterHideShow()'
    'TC_US_FUNC_014' = 'cs.verifySaveSearchAsFavorite(favorite_Label)'
    'TC_US_FUNC_015' = 'cs.verifyEditSavedFavorite(favorite_Label)'
    'TC_US_FUNC_016' = 'cs.verifyRenameSavedFavorite(favorite_Label, favorite_Label_New)'
    'TC_US_FUNC_017' = 'cs.verifyExportSearchResults()'
    'TC_US_FUNC_031' = 'cs.verifyMultipleSearchPopupOpens()'
    'TC_US_FUNC_032' = 'cs.verifyMultipleSearchScreenFields()'
    'TC_US_FUNC_033' = 'cs.verifyMultipleSearchFieldsValidation()'
    'TC_US_FUNC_034' = 'cs.verifyMultipleSearchValid(search_Criteria, search_Value)'
    'TC_US_FUNC_035' = 'cs.verifyMultipleSearchEmptyShowsError()'
    'TC_US_FUNC_036' = 'cs.verifyMultipleSearchInvalidShowsError(invalid_Search_Value)'
    'TC_US_FUNC_037' = 'cs.verifyMultipleSearchCancelClosesPopup()'
    'TC_US_FUNC_038' = 'cs.verifyNavigateToDetailPageOnRowClick()'
    'TC_US_FUNC_039' = 'cs.verifyBackFromDetailPage()'
    'TC_US_FUNC_040' = 'cs.verifySearchCriteriaDropdownValues()'
    'TC_US_FUNC_041' = 'cs.verifySearchInputForCriteria(search_Criteria, search_Value)'
    'TC_US_FUNC_042' = 'cs.verifySearchWithTrackingNumber(search_Value)'
    'TC_US_FUNC_043' = 'cs.verifySearchWithConsole(search_Value)'
    'TC_US_FUNC_044' = 'cs.verifySearchWithBoe(search_Value)'
    'TC_US_FUNC_045' = 'cs.verifySearchWithAccountNumber(search_Value)'
    'TC_US_FUNC_046' = 'cs.verifySearchWithEmptyDataShowsNoResults()'
    'TC_US_FUNC_047' = 'cs.verifySearchWithInvalidData()'
    'TC_US_FUNC_048' = 'cs.verifyDetailPageInformation()'
    'TC_Detailed_29' = 'cs.verifyPaginationAppliedCorrectlyToDataset()'
    'TC_Detailed_30' = 'cs.verifyPaginationCorrectRecordsPerPageSize()'
    'TC_Detailed_31' = 'cs.verifyPaginationServerSideLogic()'
    'TC_Detailed_32' = 'cs.verifyPaginationNextPreviousNavigation()'
    'TC_Detailed_33' = 'cs.verifyPaginationPreviousDisabledOnFirstPage()'
    'TC_Detailed_34' = 'cs.verifyPaginationNextDisabledOnLastPage()'
    'TC_Detailed_35' = 'cs.verifyPaginationAfterHardRefreshDefaultSettings()'
    'TC_Detailed_36' = 'cs.verifyPaginationPageLoadTimeBetweenPages()'
}

function Get-KeywordVariables {
    param([string]$KeywordCall)
    if ($KeywordCall -notmatch '\((.*)\)\s*$') { return @() }
    $inner = $Matches[1].Trim()
    if ([string]::IsNullOrWhiteSpace($inner)) { return @() }
    return @($inner.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function New-DefaultDataRow {
    param([string]$TestId)
    $row = [ordered]@{}
    foreach ($col in $DataColumns) { $row[$col] = '' }
    $row.ID = $TestId
    $row.charge_Group = 'Customs'
    $row.charge_Code = 'DTC'
    $row.charge_Status = 'Open'
    $row.date_Type = 'Shipment Date'
    $row.start_Date = '01/01/2024'
    $row.end_Date = '31/12/2024'
    $row.search_Criteria = 'Tracking Number'
    $row.search_Value = '35600000001'
    $row.tracking_Numbers = '35600000001|35600000002'
    $row.invalid_Tracking_Numbers = 'INVALID@@'
    $row.invalid_Search_Value = 'BAD@@VALUE'
    $row.favorite_Label = 'AutoFavoriteSearch'
    $row.favorite_Label_New = 'AutoFavoriteSearchRenamed'
    $row.entity_Filter = 'RUH'

    switch -Regex ($TestId) {
        '^TC_US_FUNC_007$' { $row.start_Date = '31/12/2024'; $row.end_Date = '01/01/2024'; break }
        '^TC_US_FUNC_043$' { $row.search_Criteria = 'Console'; $row.search_Value = 'CONSOLE001'; break }
        '^TC_US_FUNC_044$' { $row.search_Criteria = 'BOE'; $row.search_Value = 'BOE123456'; break }
        '^TC_US_FUNC_045$' { $row.search_Criteria = 'Account Number'; $row.search_Value = '80001234'; break }
        '^TC_US_FUNC_041$' { $row.search_Criteria = 'Tracking Number'; $row.search_Value = '35600000001'; break }
        '^TC_US_FUNC_034$' { $row.search_Criteria = 'Tracking Number'; $row.search_Value = '35600000001'; break }
        '^TC_US_FUNC_046$' { $row.search_Value = ''; break }
        '^TC_US_FUNC_047$' { $row.search_Value = 'INVALID_NO_MATCH_XYZ'; break }
    }
    return [pscustomobject]$row
}

function Write-DataDatFile {
    if (Test-Path $DataDatPath) { return }
    $dat = @(
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<DataFileEntity>',
        '   <description>Custom Search CCIPH-21 test data. Source: Data Files/CustomSearch_CCIPH21_Data.csv</description>',
        '   <name>CustomSearch_CCIPH21_Data</name>',
        '   <tag>CCIPH-21,CustomSearch,360ViewScreen</tag>',
        '   <connectionProperties/>',
        '   <containsHeaders>true</containsHeaders>',
        '   <csvSeperator>COMMA</csvSeperator>',
        '   <dataFileGUID>cs-cciph21-data-4a1b-9c0d-8e7f-000000000021</dataFileGUID>',
        '   <dataSourceUrl>CustomSearch_CCIPH21_Data.csv</dataSourceUrl>',
        '   <driver>CSV</driver>',
        '   <isInternalPath>true</isInternalPath>',
        '   <query></query>',
        '   <secureUserAccount>false</secureUserAccount>',
        '   <sheetName></sheetName>',
        '   <usingGlobalDBSetting>false</usingGlobalDBSetting>',
        '</DataFileEntity>'
    ) -join [Environment]::NewLine
    Set-Content -Path $DataDatPath -Value $dat -Encoding UTF8
}

function Escape-Xml {
    param([string]$Text)
    if ($null -eq $Text) { return '' }
    return ($Text -replace '&','&amp;' -replace '<','&lt;' -replace '>','&gt;' -replace '"','&quot;' -replace '''','&apos;')
}

function New-TestCaseTcContent {
    param(
        [string]$TestId,
        [string]$Description,
        [int]$Iteration,
        [string[]]$Variables
    )
    $linkId = Get-TestDataLinkId -TestId $TestId
    $guid = Get-TestCaseGuid -TestId $TestId
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine('<TestCaseEntity>')
    [void]$sb.AppendLine("   <description>$(Escape-Xml $Description) (CCIPH-21 / $TestId)</description>")
    [void]$sb.AppendLine("   <name>$TestId</name>")
    [void]$sb.AppendLine('   <tag>GenAI,CCIPH-21,CustomSearch,360ViewScreen</tag>')
    [void]$sb.AppendLine('   <comment>CSV: CustomSearch_360ViewScreen.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-21</comment>')
    [void]$sb.AppendLine('   <origin>GenAI</origin>')
    [void]$sb.AppendLine('   <recordOption>OTHER</recordOption>')
    [void]$sb.AppendLine("   <testCaseGuid>$guid</testCaseGuid>")
    [void]$sb.AppendLine('   <testDataLinks>')
    [void]$sb.AppendLine('      <combinationType>ONE</combinationType>')
    [void]$sb.AppendLine("      <id>$linkId</id>")
    [void]$sb.AppendLine('      <iterationEntity>')
    [void]$sb.AppendLine('         <iterationType>SPECIFIC</iterationType>')
    [void]$sb.AppendLine("         <value>$Iteration</value>")
    [void]$sb.AppendLine('      </iterationEntity>')
    [void]$sb.AppendLine("      <testDataId>$DataId</testDataId>")
    [void]$sb.AppendLine('   </testDataLinks>')
    foreach ($var in $Variables) {
        [void]$sb.AppendLine('   <variableLinks>')
        [void]$sb.AppendLine("      <testDataLinkId>$linkId</testDataLinkId>")
        [void]$sb.AppendLine('      <type>DATA_COLUMN</type>')
        [void]$sb.AppendLine("      <value>$var</value>")
        [void]$sb.AppendLine("      <variableId>$(Get-VariableId -Name $var)</variableId>")
        [void]$sb.AppendLine('   </variableLinks>')
    }
    foreach ($var in $Variables) {
        [void]$sb.AppendLine('   <variable>')
        [void]$sb.AppendLine("      <defaultValue>''</defaultValue>")
        [void]$sb.AppendLine('      <description></description>')
        [void]$sb.AppendLine("      <id>$(Get-VariableId -Name $var)</id>")
        [void]$sb.AppendLine('      <masked>false</masked>')
        [void]$sb.AppendLine("      <name>$var</name>")
        [void]$sb.AppendLine('   </variable>')
    }
    [void]$sb.AppendLine('</TestCaseEntity>')
    return $sb.ToString()
}

function New-GroovyScriptContent {
    param([string]$TestId, [string]$KeywordCall)
    return @"
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | $TestId

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToCustomSearchScreen()
$KeywordCall
WebUI.takeScreenshot()
WebUI.closeBrowser()
"@
}

if (-not (Test-Path $SourceCsv)) { throw "Source CSV not found: $SourceCsv" }

$rows = Import-Csv $SourceCsv | Where-Object { $_.'User Story ID' -eq 'CCIPH-21' }
if (-not $rows -or $rows.Count -eq 0) { throw 'No CCIPH-21 rows found in source CSV.' }

$dataRows = @()
foreach ($r in $rows) {
    $id = $r.ID.Trim()
    if (-not $KeywordMap.ContainsKey($id)) {
        Write-Warning "No keyword mapping for $id - skipping."
        continue
    }
    $dataRows += New-DefaultDataRow -TestId $id
}

$dataRows | Export-Csv -Path $DataCsvPath -NoTypeInformation -Encoding UTF8
Write-DataDatFile

New-Item -ItemType Directory -Force -Path $TcRoot, $ScriptRoot | Out-Null

$created = New-Object System.Collections.Generic.List[string]
$iterationById = @{}
for ($i = 0; $i -lt $dataRows.Count; $i++) {
    $iterationById[$dataRows[$i].ID] = $i + 1
}

foreach ($r in $rows) {
    $id = $r.ID.Trim()
    if (-not $KeywordMap.ContainsKey($id)) { continue }
    if (-not $iterationById.ContainsKey($id)) { continue }

    $desc = ($r.'Test Case Description' -replace '\s+', ' ').Trim()
    $keywordCall = $KeywordMap[$id]
    $vars = @(Get-KeywordVariables -KeywordCall $keywordCall)
    $iteration = $iterationById[$id]

    $tcPath = Join-Path $TcRoot "$id.tc"
    $tcContent = New-TestCaseTcContent -TestId $id -Description $desc -Iteration $iteration -Variables $vars
    Set-Content -Path $tcPath -Value $tcContent -Encoding UTF8
    $created.Add($tcPath)

    $scriptDir = Join-Path $ScriptRoot $id
    New-Item -ItemType Directory -Force -Path $scriptDir | Out-Null
    Get-ChildItem -Path $scriptDir -Filter 'Script*.groovy' -ErrorAction SilentlyContinue | Remove-Item -Force
    $scriptPath = Join-Path $scriptDir (Get-ScriptFileName -TestId $id)
    $groovy = New-GroovyScriptContent -TestId $id -KeywordCall $keywordCall
    Set-Content -Path $scriptPath -Value $groovy -Encoding UTF8
    $created.Add($scriptPath)
}

$created.Add($DataCsvPath)
if (Test-Path $DataDatPath) { $created.Add($DataDatPath) }

Write-Host "Generated $($created.Count) files for CCIPH-21."
$created | ForEach-Object { Write-Host $_ }




