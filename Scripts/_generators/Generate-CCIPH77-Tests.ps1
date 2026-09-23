# Generates CCIPH-77 test cases and scripts from ChargeCreation-TestCases.csv filter
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
    $root = Split-Path $PSScriptRoot -Parent
    if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
        $root = (Get-Location).Path
    }
}

$tcRoot = Join-Path $root 'Test Cases\ChargeCreation\CCIPH-77'
$scriptRoot = Join-Path $root 'Scripts\ChargeCreation\CCIPH-77'
New-Item -ItemType Directory -Force -Path $tcRoot | Out-Null
New-Item -ItemType Directory -Force -Path $scriptRoot | Out-Null

$columns = @(
    'tracking_Number','no_Shipper_Tracking_Number','missing_Mapping_Tracking_Number',
    'ar_Rate_Tracking_Number','entity_Rate_Tracking_Number','seller_Rate_Tracking_Number',
    'no_Rate_Tracking_Number','fixed_Rate_Tracking_Number','percent_Rate_Tracking_Number',
    'shipper_Number','entity','debiting_Entity','expected_Billing_Account','expected_Billing_Context',
    'charge_Group','charge_Code','expected_Status','expected_Rate_Source','flow_Type','login_Required',
    'estimate_File_Path','custom_Duty','import_Vat','other_Charges','custom_Duty_Updated',
    'import_Vat_Updated','other_Charges_Updated','custom_Duty_Actual','rerun_Count','skip_Reason'
)

$tests = @(
    @{ Id='TC_DTA_001'; Num=77001; Desc='Verify DTA charge record created for qualifying shipment' },
    @{ Id='TC_DTA_002'; Num=77002; Desc='Verify billing context Intercompany and assigned to ASL001' },
    @{ Id='TC_DTA_003'; Num=77003; Desc='Verify error when shipper account unavailable' },
    @{ Id='TC_DTA_004'; Num=77004; Desc='Verify debiting entity derived from origin and third-party mapping' },
    @{ Id='TC_DTA_005'; Num=77005; Desc='Verify error when debiting entity mapping missing' },
    @{ Id='TC_DTA_006'; Num=77006; Desc='Verify AR contract rate applied with highest priority' },
    @{ Id='TC_DTA_007'; Num=77007; Desc='Verify entity public rate used when AR contract not available' },
    @{ Id='TC_DTA_008'; Num=77008; Desc='Verify seller rate used when AR and entity rates unavailable' },
    @{ Id='TC_DTA_009'; Num=77009; Desc='Verify error when no rates available' },
    @{ Id='TC_DTA_010'; Num=77010; Desc='Verify all revenue elements calculated from selected rate sheet' },
    @{ Id='TC_DTA_011'; Num=77011; Desc='Verify fixed rate calculated once during creation' },
    @{ Id='TC_DTA_012'; Num=77012; Desc='Verify fixed rate not recalculated after estimate update' },
    @{ Id='TC_DTA_013'; Num=77013; Desc='Verify percentage rate calculated using defined identifier' },
    @{ Id='TC_DTA_014'; Num=77014; Desc='Verify percentage rate recalculated after estimate update' },
    @{ Id='TC_DTA_015'; Num=77015; Desc='Verify percentage rate recalculated after actual update' },
    @{ Id='TC_DTA_DB_001'; Num=77016; Desc='Validate DTA row created in DB' },
    @{ Id='TC_DTA_DB_002'; Num=77017; Desc='Validate charge code stored as DTA' },
    @{ Id='TC_DTA_DB_003'; Num=77018; Desc='Validate billing account stored as ASL001' },
    @{ Id='TC_DTA_DB_004'; Num=77019; Desc='Validate debiting entity stored correctly' },
    @{ Id='TC_DTA_DB_005'; Num=77020; Desc='Validate selected rate source stored' },
    @{ Id='TC_DTA_DB_006'; Num=77021; Desc='Validate fixed amount persisted correctly' },
    @{ Id='TC_DTA_DB_007'; Num=77022; Desc='Validate recalculated amount updated for percentage rate' },
    @{ Id='TC_DTA_DB_008'; Num=77023; Desc='Validate no duplicate DTA rows on reprocess' },
    @{ Id='TC_DTA_DB_009'; Num=77024; Desc='Validate audit fields updated on recalculation' },
    @{ Id='TC_DTA_UI_001'; Num=77025; Desc='Verify DTA values on Shipment Detailed Page' },
    @{ Id='TC_DTA_UI_002'; Num=77026; Desc='Validate Shipment Detailed Page values with DB' }
)

function New-VariableLinks($tcId) {
    $linkId = "cc77-link-$($tcId.ToLower())"
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc77-var-$($col.ToLower())"
        $sb += "   <variableLinks>`n      <testDataLinkId>$linkId</testDataLinkId>`n      <type>DATA_COLUMN</type>`n      <value>$col</value>`n      <variableId>$varId</variableId>`n   </variableLinks>"
    }
    return $sb -join ''
}

function New-Variables {
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc77-var-$($col.ToLower())"
        $sb += "   <variable>`n      <defaultValue>''</defaultValue>`n      <description></description>`n      <id>$varId</id>`n      <masked>false</masked>`n      <name>$col</name>`n   </variable>"
    }
    return $sb -join ''
}

function New-ScriptGroovy($tcId) {
    $bindingLines = $columns | ForEach-Object { "`t$_ : $_," }
    $bindingBlock = ($bindingLines -join "`n").TrimEnd(',')
    return @"
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = '$tcId'
// CSV: ChargeCreation-TestCases.csv | CCIPH-77 | $tcId

def cc = new CC()
cc.executeDtaCalculationTest(tcId, [
$bindingBlock
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
"@
}

foreach ($t in $tests) {
    $tcId = $t.Id
    $num = $t.Num
    $desc = $t.Desc
    $guid = "cc77-$($tcId.ToLower())-0001"
    $linkId = "cc77-link-$($tcId.ToLower())"

    $tcXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>$desc (CCIPH-77 / $tcId)</description>
   <name>$tcId</name>
   <tag>GenAI,Critical,CCIPH-77,ChargeCreation,DTA</tag>
   <comment>CSV: ChargeCreation-TestCases.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-77 | $tcId</comment>
   <origin>GenAI</origin>
   <recordOption>OTHER</recordOption>
   <testCaseGuid>$guid</testCaseGuid>
   <testDataLinks>
      <combinationType>ONE</combinationType>
      <id>$linkId</id>
      <iterationEntity>
         <iterationType>SPECIFIC</iterationType>
         <value>1</value>
      </iterationEntity>
      <testDataId>Data Files/ChargeCreation_CCIPH77_Data</testDataId>
   </testDataLinks>
$(New-VariableLinks $tcId)
$(New-Variables)
</TestCaseEntity>
"@

    $tcPath = Join-Path $tcRoot "$tcId.tc"
    Set-Content -Path $tcPath -Value $tcXml -Encoding UTF8

    $scriptDir = Join-Path $scriptRoot $tcId
    New-Item -ItemType Directory -Force -Path $scriptDir | Out-Null
    $scriptPath = Join-Path $scriptDir "Script$num.groovy"
    Set-Content -Path $scriptPath -Value (New-ScriptGroovy $tcId) -Encoding UTF8
    Write-Host "Created $tcId"
}

Write-Host "Done. Generated $($tests.Count) test cases under CCIPH-77."
