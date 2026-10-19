$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$CsvSource = Join-Path $Root "Shipment Creation-TestCases.csv"
$TcDir = Join-Path $Root "Test Cases\ShipmentCreation\CCIPH-42"
$ScriptDir = Join-Path $Root "Scripts\ShipmentCreation\CCIPH-42"
$DataCsv = Join-Path $Root "Data Files\ShipmentCreation_CCIPH42_Data.csv"
$DataDat = Join-Path $Root "Data Files\ShipmentCreation_CCIPH42_Data.dat"
$SuiteFile = Join-Path $Root "Test Suites\ShipmentCreation-TEST_SUITE.ts"

$DataColumns = @(
    "ID", "product_Group", "shipment_Status", "account_Number", "charge_Group", "charge_Code",
    "entity", "rule_Scenario", "expected_Behavior", "expected_Charge_Codes", "expected_Status",
    "category_Rule", "expected_Category", "tracking_Number"
)

$CategoryExpected = @{
    "001"="Exempted";"002"="Not Exempted";"003"="Exempted";"004"="Exempted";"005"="Exempted";"006"="Exempted"
    "007"="Not Exempted";"008"="Exempted";"009"="Exempted";"010"="B2B";"011"="HV";"012"="HV";"013"="LV"
    "014"="LV";"015"="HV";"016"="HV";"017"="HV";"018"="LV";"019"="LV";"020"="HV";"021"="HV";"022"="LV"
    "023"="LV";"024"="HV";"025"="Boundary";"026"="Exempted";"027"="Single";"028"="HV"
}

$BindingsByTc = @{
    "TC_US_FUNC_001" = @("product_Group", "shipment_Status")
    "TC_US_FUNC_002" = @("product_Group", "shipment_Status", "account_Number")
    "TC_US_FUNC_003" = @("charge_Group", "charge_Code", "entity")
    "TC_US_FUNC_004" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_005" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_006" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_007" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_008" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_009" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_010" = @("rule_Scenario", "expected_Behavior")
    "TC_US_FUNC_011" = @("expected_Behavior")
    "TC_US_FUNC_012" = @("rule_Scenario", "expected_Behavior", "charge_Group", "charge_Code")
    "TC_US_FUNC_013" = @("expected_Behavior", "charge_Group", "charge_Code")
    "TC_US_FUNC_014" = @("rule_Scenario", "expected_Behavior", "charge_Group", "charge_Code")
    "TC_US_FUNC_015" = @("rule_Scenario", "expected_Charge_Codes")
    "TC_US_FUNC_016" = @("rule_Scenario", "expected_Charge_Codes", "charge_Group")
    "TC_US_FUNC_017" = @("charge_Code")
    "TC_US_FUNC_018" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_019" = @("charge_Code")
    "TC_US_FUNC_020" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_021" = @("charge_Code")
    "TC_US_FUNC_022" = @("charge_Code")
    "TC_US_FUNC_023" = @("charge_Code")
    "TC_US_FUNC_024" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_025" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_026" = @("charge_Code")
    "TC_US_FUNC_027" = @("charge_Code")
    "TC_US_FUNC_028" = @("charge_Code")
    "TC_US_FUNC_029" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_030" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_031" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_034" = @("charge_Code", "charge_Group", "entity")
    "TC_US_FUNC_037" = @()
    "TC_US_FUNC_038" = @("expected_Status")
    "TC_US_FUNC_039" = @("expected_Status")
    "TC_US_FUNC_040" = @("expected_Status")
    "TC_US_FUNC_041" = @("expected_Status")
    "TC_US_FUNC_042" = @("product_Group")
}

function Get-FlowData($tcId) {
    switch ($tcId) {
        "TC_US_FUNC_001" { return @{ product_Group="EXP"; shipment_Status="Full" } }
        "TC_US_FUNC_002" { return @{ product_Group="DOM"; shipment_Status="Full"; account_Number="72104493" } }
        "TC_US_FUNC_003" { return @{ charge_Group="Cost"; charge_Code="ECD"; entity="RUH" } }
        "TC_US_FUNC_004" { return @{ rule_Scenario="RULE_1"; expected_Behavior="RULE_1_ONLY" } }
        "TC_US_FUNC_005" { return @{ rule_Scenario="RULE_2"; expected_Behavior="RULE_2_ONLY" } }
        "TC_US_FUNC_006" { return @{ rule_Scenario="RULE_3"; expected_Behavior="RULE_3_ONLY" } }
        "TC_US_FUNC_007" { return @{ rule_Scenario="RULE_1_AND_2"; expected_Behavior="RULE_1_ONLY" } }
        "TC_US_FUNC_008" { return @{ rule_Scenario="RULE_2_AND_3"; expected_Behavior="RULE_2_ONLY" } }
        "TC_US_FUNC_009" { return @{ rule_Scenario="ALL_RULES"; expected_Behavior="RULE_1_ONLY" } }
        "TC_US_FUNC_010" { return @{ rule_Scenario="RULE_1_EXPIRED"; expected_Behavior="ALL_RECORDS" } }
        "TC_US_FUNC_011" { return @{ expected_Behavior="TODAYS_ONLY" } }
        "TC_US_FUNC_012" { return @{ rule_Scenario="DDP_RULE_1"; expected_Behavior="RULE_1_ONLY"; charge_Group="Revenue"; charge_Code="DTA" } }
        "TC_US_FUNC_013" { return @{ expected_Behavior="TODAYS_ONLY"; charge_Group="Revenue"; charge_Code="DTA" } }
        "TC_US_FUNC_014" { return @{ rule_Scenario="DDP_EXPIRED"; expected_Behavior="ALL_RECORDS"; charge_Group="Revenue"; charge_Code="DTA" } }
        "TC_US_FUNC_015" { return @{ rule_Scenario="RULE_1"; expected_Charge_Codes="ECD,DTC" } }
        "TC_US_FUNC_016" { return @{ rule_Scenario="DDP_RULE_1"; expected_Charge_Codes="ECD,DTA,ECM"; charge_Group="Cost" } }
        "TC_US_FUNC_017" { return @{ charge_Code="ECD" } }
        "TC_US_FUNC_018" { return @{ charge_Code="ECD"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_019" { return @{ charge_Code="ECD" } }
        "TC_US_FUNC_020" { return @{ charge_Code="ECD"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_021" { return @{ charge_Code="DTC" } }
        "TC_US_FUNC_022" { return @{ charge_Code="DTC" } }
        "TC_US_FUNC_023" { return @{ charge_Code="DTC" } }
        "TC_US_FUNC_024" { return @{ charge_Code="ECD"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_025" { return @{ charge_Code="ECD"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_026" { return @{ charge_Code="DTA" } }
        "TC_US_FUNC_027" { return @{ charge_Code="DTA" } }
        "TC_US_FUNC_028" { return @{ charge_Code="DTA" } }
        "TC_US_FUNC_029" { return @{ charge_Code="ECM"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_030" { return @{ charge_Code="ECM"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_031" { return @{ charge_Code="DT3"; charge_Group="Revenue"; entity="RUH" } }
        "TC_US_FUNC_034" { return @{ charge_Code="ECM"; charge_Group="Cost"; entity="RUH" } }
        "TC_US_FUNC_038" { return @{ expected_Status="PENDING CALCULATION" } }
        "TC_US_FUNC_039" { return @{ expected_Status="DUE" } }
        "TC_US_FUNC_040" { return @{ expected_Status="SETTLED" } }
        "TC_US_FUNC_041" { return @{ expected_Status="TERMINATED" } }
        "TC_US_FUNC_042" { return @{ product_Group="EXP" } }
        default {
            if ($tcId -like "TC_CAT_FUNC_*") {
                $num = $tcId.Replace("TC_CAT_FUNC_", "")
                return @{
                    category_Rule = "GS-VC-$($num.PadLeft(2,'0'))"
                    expected_Category = $CategoryExpected[$num]
                    entity = "RUH"
                    tracking_Number = "35600000001"
                }
            }
            return @{}
        }
    }
}

function Get-Bindings($tcId) {
    if ($BindingsByTc.ContainsKey($tcId)) { return $BindingsByTc[$tcId] }
    if ($tcId -like "TC_CAT_FUNC_*") { return @("category_Rule", "expected_Category", "tracking_Number", "entity") }
    return @()
}

function Get-TcSlug($tcId) {
    return $tcId.ToLower().Replace("_", "-")
}

function Get-ScriptName($tcId) {
    if ($tcId -match 'TC_US_FUNC_(\d+)') { return "Script420$($matches[1].PadLeft(3,'0'))" }
    if ($tcId -match 'TC_CAT_FUNC_(\d+)') { return "Script4201$(([int]$matches[1]).ToString('00'))" }
    return "Script420000"
}

function Get-VarId($column) {
    $suffix = $column -creplace '([a-z])([A-Z])', '$1-$2'
    return "sc42-var-$suffix".ToLower()
}

function New-TcXml($tcId, $description, $rowIndex, $priority) {
    $slug = Get-TcSlug $tcId
    $guid = "sc-cciph42-$slug-0001"
    $linkId = "sc42-link-$slug"
    $bindings = Get-Bindings $tcId
    $desc = ($description.Replace('"', "'") + " (CCIPH-42 / $tcId)")
    $prioTag = if ($priority) { ",$priority" } else { "" }

    $linkBlocks = @()
    $varBlocks = @()
    if ($bindings.Count -gt 0) {
        foreach ($col in $bindings) {
            $varId = Get-VarId $col
            $linkBlocks += @"
   <variableLinks>
      <testDataLinkId>$linkId</testDataLinkId>
      <type>DATA_COLUMN</type>
      <value>$col</value>
      <variableId>$varId</variableId>
   </variableLinks>
"@
            $varBlocks += @"
   <variable>
      <defaultValue>''</defaultValue>
      <description></description>
      <id>$varId</id>
      <masked>false</masked>
      <name>$col</name>
   </variable>
"@
        }
    }

    $dataLinkBlock = ""
    if ($bindings.Count -gt 0) {
        $dataLinkBlock = @"
   <testDataLinks>
      <combinationType>ONE</combinationType>
      <id>$linkId</id>
      <iterationEntity>
         <iterationType>SPECIFIC</iterationType>
         <value>$rowIndex</value>
      </iterationEntity>
      <testDataId>Data Files/ShipmentCreation_CCIPH42_Data</testDataId>
   </testDataLinks>
"@
    }

    return @"
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>$desc</description>
   <name>$tcId</name>
   <tag>GenAI$prioTag,CCIPH-42,ShipmentCreation</tag>
   <comment>CSV: Shipment Creation-TestCases.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-42</comment>
   <origin>GenAI</origin>
   <recordOption>OTHER</recordOption>
   <testCaseGuid>$guid</testCaseGuid>
$dataLinkBlock
$($linkBlocks -join "`n")
$($varBlocks -join "`n")
</TestCaseEntity>
"@
}

function New-ScriptBody($tcId) {
    $header = @"
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = '$tcId'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | $tcId

def sc = new SC()
"@

    $loginBlock = @"

def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
"@

    $footer = @"

WebUI.takeScreenshot()
WebUI.closeBrowser()
"@

    $dbFooter = @"

WebUI.takeScreenshot()
"@

    switch ($tcId) {
        "TC_US_FUNC_001" {
            return $header + "`nsc.verifyEligibilityCriteriaInDb(product_Group, shipment_Status)" + $dbFooter
        }
        "TC_US_FUNC_002" {
            return $header + "`nsc.verifyEligibilityCriteriaInDb(product_Group, shipment_Status, account_Number)" + $dbFooter
        }
        "TC_US_FUNC_003" {
            return $header + $loginBlock + "`nsc.verifyMandatoryShipmentFieldsOnCustomScreen(charge_Group, charge_Code, entity)" + $footer
        }
        "TC_US_FUNC_004" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_005" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_006" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_007" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_008" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_009" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_010" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_011" {
            return $header + $loginBlock + "`n'Navigate to Global Settings and edit rule'`nsc.navigateToGlobalSettingsAndVerifyLoaded()`nsc.editFirstGlobalRuleAndSave()`nsc.validateCustomScreenForRule(expected_Behavior)" + $footer
        }
        "TC_US_FUNC_012" {
            return $header + $loginBlock + "`nsc.createDdpGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior, charge_Group, charge_Code)" + $footer
        }
        "TC_US_FUNC_013" {
            return $header + $loginBlock + "`nsc.navigateToGlobalSettingsAndVerifyLoaded()`nsc.editFirstGlobalRuleAndSave()`nsc.validateCustomScreenForRule(expected_Behavior, charge_Group, charge_Code)" + $footer
        }
        "TC_US_FUNC_014" {
            return $header + $loginBlock + "`nsc.createDdpGlobalRule(rule_Scenario)`nsc.validateCustomScreenForRule(expected_Behavior, charge_Group, charge_Code)" + $footer
        }
        "TC_US_FUNC_015" {
            return $header + $loginBlock + "`nsc.createDduGlobalRule(rule_Scenario)`nsc.verifyChargeCodesOnCustomScreen(expected_Charge_Codes)" + $footer
        }
        "TC_US_FUNC_016" {
            return $header + $loginBlock + "`nsc.createDdpGlobalRule(rule_Scenario)`nsc.verifyChargeCodesOnCustomScreen(expected_Charge_Codes, charge_Group)" + $footer
        }
        "TC_US_FUNC_017" {
            return $header + "`nsc.verifyAccountNumberInDb(charge_Code, 'PRESENT')" + $dbFooter
        }
        "TC_US_FUNC_018" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, true)" + $footer
        }
        "TC_US_FUNC_019" {
            return $header + "`nsc.verifyAccountNumberInDb(charge_Code, 'BLANK')" + $dbFooter
        }
        "TC_US_FUNC_020" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, false)" + $footer
        }
        "TC_US_FUNC_021" {
            return $header + "`nsc.verifyAccountStatusAndOutstanding(charge_Code, 'Active', 'AR')" + $dbFooter
        }
        "TC_US_FUNC_022" {
            return $header + "`nsc.verifyAccountStatusAndOutstanding(charge_Code, 'Blocked', 'Entity Management')" + $dbFooter
        }
        "TC_US_FUNC_023" {
            return $header + "`nsc.verifyOutstandingFromEntityManagement(charge_Code)" + $dbFooter
        }
        "TC_US_FUNC_024" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, true)" + $footer
        }
        "TC_US_FUNC_025" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, false)" + $footer
        }
        "TC_US_FUNC_026" {
            return $header + "`nsc.verifyAccountStatusAndOutstanding(charge_Code, 'Active', 'AR')" + $dbFooter
        }
        "TC_US_FUNC_027" {
            return $header + "`nsc.verifyAccountStatusAndOutstanding(charge_Code, 'Blocked', 'Entity Management')" + $dbFooter
        }
        "TC_US_FUNC_028" {
            return $header + "`nsc.verifyOutstandingFromEntityManagement(charge_Code)" + $dbFooter
        }
        "TC_US_FUNC_029" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, true)" + $footer
        }
        "TC_US_FUNC_030" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, false)" + $footer
        }
        "TC_US_FUNC_031" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, false)" + $footer
        }
        "TC_US_FUNC_034" {
            return $header + $loginBlock + "`nsc.verifyTotalOutstandingInDbAndUi(charge_Code, charge_Group, entity, false)" + $footer
        }
        "TC_US_FUNC_037" {
            return $header + $loginBlock + "`nsc.verifyPaymentStatusOnCustomScreen('Due,Settled,Pending calculation,Terminated,Cancelled')" + $footer
        }
        "TC_US_FUNC_038" {
            return $header + $loginBlock + "`nsc.verifyPaymentStatusOnCustomScreen(expected_Status)" + $footer
        }
        "TC_US_FUNC_039" {
            return $header + $loginBlock + "`nsc.verifyPaymentStatusOnCustomScreen(expected_Status)" + $footer
        }
        "TC_US_FUNC_040" {
            return $header + $loginBlock + "`nsc.verifyPaymentStatusOnCustomScreen(expected_Status)" + $footer
        }
        "TC_US_FUNC_041" {
            return $header + $loginBlock + "`nsc.verifyPaymentStatusOnCustomScreen(expected_Status)" + $footer
        }
        "TC_US_FUNC_042" {
            return $header + "`nsc.verifyExemptionInDb('SA', 'SA', product_Group, 'TERMINATED')" + $dbFooter
        }
        default {
            if ($tcId -like "TC_CAT_FUNC_*") {
                return $header + $loginBlock + @"

'Validate category rule on Global Settings and Custom Search'
sc.validateCategoryRule(category_Rule, expected_Category, [tracking_Number: tracking_Number, entity: entity])
"@ + $footer
            }
            return $header + "`n// TODO: map script for $tcId" + $footer
        }
    }
}

New-Item -ItemType Directory -Force -Path $TcDir, $ScriptDir | Out-Null

$rows = Import-Csv $CsvSource | Where-Object { $_.'User Story ID' -eq 'CCIPH-42' }
$dataLines = @()
$dataLines += '"' + ($DataColumns -join '","') + '"'
$index = 1
$links = @()

foreach ($row in $rows) {
    $tcId = $row.ID.Trim()
    $desc = if ($row.'Test Case Description') { $row.'Test Case Description' } else { $tcId }
    $priority = if ($row.Priority) { $row.Priority } else { "Critical" }

    (New-TcXml $tcId $desc $index $priority) | Set-Content -Path (Join-Path $TcDir "$tcId.tc") -Encoding UTF8

    $scriptBody = New-ScriptBody $tcId
    $scriptName = Get-ScriptName $tcId
    $folder = Join-Path $ScriptDir $tcId
    New-Item -ItemType Directory -Force -Path $folder | Out-Null

    Get-ChildItem -Path $folder -Filter "Script*.groovy" -ErrorAction SilentlyContinue | Remove-Item -Force
    $scriptBody | Set-Content -Path (Join-Path $folder "$scriptName.groovy") -Encoding UTF8 -NoNewline
    Add-Content -Path (Join-Path $folder "$scriptName.groovy") -Value "" -Encoding UTF8

    $flow = Get-FlowData $tcId
    $vals = @($tcId)
    foreach ($col in $DataColumns[1..($DataColumns.Length - 1)]) {
        if ($flow.ContainsKey($col)) { $vals += $flow[$col] } else { $vals += "" }
    }
    $dataLines += '"' + ($vals -join '","') + '"'

    $links += @"
   <testCaseLink>
      <guid>$([guid]::NewGuid())</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ShipmentCreation/CCIPH-42/$tcId</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
"@
    $index++
}

$dataLines | Set-Content -Path $DataCsv -Encoding UTF8

@'
<?xml version="1.0" encoding="UTF-8"?>
<DataFileEntity>
   <description>Shipment Creation CCIPH-42 test data. Source: Data Files/ShipmentCreation_CCIPH42_Data.csv</description>
   <name>ShipmentCreation_CCIPH42_Data</name>
   <tag>CCIPH-42,ShipmentCreation</tag>
   <connectionProperties/>
   <containsHeaders>true</containsHeaders>
   <csvSeperator>COMMA</csvSeperator>
   <dataFileGUID>sc-cciph42-data-4a1b-9c0d-8e7f-000000000042</dataFileGUID>
   <dataSourceUrl>ShipmentCreation_CCIPH42_Data.csv</dataSourceUrl>
   <driver>CSV</driver>
   <isInternalPath>true</isInternalPath>
   <query></query>
   <secureUserAccount>false</secureUserAccount>
   <sheetName></sheetName>
   <usingGlobalDBSetting>false</usingGlobalDBSetting>
</DataFileEntity>
'@ | Set-Content -Path $DataDat -Encoding UTF8

$suite = @"
<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Shipment Creation CCIPH-42 — eligibility, DDU/DDP charge creation, category rules. Chrome. Run LOGIN-TEST_SUITE first. Configure CCI_DB_* profile globals for DB tests. Data: Data Files/ShipmentCreation_CCIPH42_Data.csv</description>
   <name>ShipmentCreation-TEST_SUITE</name>
   <tag>CCIPH-42,ShipmentCreation,GenAI</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>sc-cciph42-suite-0001</testSuiteGuid>
$($links -join "`n")
</TestSuiteEntity>
"@
$suite | Set-Content -Path $SuiteFile -Encoding UTF8

Write-Host "Regenerated $($rows.Count) ShipmentCreation test cases in standard module format"
