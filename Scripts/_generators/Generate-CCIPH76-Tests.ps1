# Generates CCIPH-76 test cases and scripts from ChargeCreation-TestCases.csv filter
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
    $root = Split-Path $PSScriptRoot -Parent
    if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
        $root = (Get-Location).Path
    }
}

$tcRoot = Join-Path $root 'Test Cases\ChargeCreation\CCIPH-76'
$scriptRoot = Join-Path $root 'Scripts\ChargeCreation\CCIPH-76'
New-Item -ItemType Directory -Force -Path $tcRoot | Out-Null
New-Item -ItemType Directory -Force -Path $scriptRoot | Out-Null

$columns = @(
    'tracking_Number','cash_Tracking_Number','account_Tracking_Number','account_Fallback_Tracking_Number',
    'no_Rate_Tracking_Number','fixed_Rate_Tracking_Number','percent_Rate_Tracking_Number','non_Invoiced_Tracking_Number',
    'shipper_Number','entity','charge_Group','charge_Code','expected_Status','payment_Type','expected_Rate_Source',
    'flow_Type','login_Required','estimate_File_Path','custom_Duty','import_Vat','other_Charges',
    'custom_Duty_Updated','import_Vat_Updated','other_Charges_Updated','expected_Contract_Rate','rerun_Count','skip_Reason'
)

$tests = @(
    @{ Id='TC_DTC_001'; Num=76001; Desc='Verify DTC charge record created for qualifying shipment' },
    @{ Id='TC_DTC_002'; Num=76002; Desc='Verify Cash shipment uses entity public rate' },
    @{ Id='TC_DTC_003'; Num=76003; Desc='Verify Account shipment uses AR contract rate' },
    @{ Id='TC_DTC_004'; Num=76004; Desc='Verify Account shipment fallback to entity rate' },
    @{ Id='TC_DTC_005'; Num=76005; Desc='Verify error when no rate sheet configured' },
    @{ Id='TC_DTC_006'; Num=76006; Desc='Verify revenue elements calculated for selected rate sheet' },
    @{ Id='TC_DTC_007'; Num=76007; Desc='Verify fixed rate not recalculated after estimate update' },
    @{ Id='TC_DTC_008'; Num=76008; Desc='Verify percentage rate recalculated after estimate change' },
    @{ Id='TC_DTC_009'; Num=76009; Desc='Verify non invoiced charge recalculates on ECD update' },
    @{ Id='TC_DTC_010'; Num=76010; Desc='Verify settled charge uses variance on actual update' },
    @{ Id='TC_DTC_DB_001'; Num=76011; Desc='Validate DTC row created in DB' },
    @{ Id='TC_DTC_DB_002'; Num=76012; Desc='Validate charge code as DTC' },
    @{ Id='TC_DTC_DB_003'; Num=76013; Desc='Validate rate source stored as AR/Entity' },
    @{ Id='TC_DTC_DB_004'; Num=76014; Desc='Validate no duplicate rows on reprocess' },
    @{ Id='TC_DTC_DB_005'; Num=76015; Desc='Validate updated amount after recalculation' },
    @{ Id='TC_DTC_DB_006'; Num=76016; Desc='Validate variance row created' },
    @{ Id='TC_DTC_UI_001'; Num=76017; Desc='Verify DTC values on Shipment Detail page' },
    @{ Id='TC_DTC_UI_002'; Num=76018; Desc='Validate DTC UI values with DB' }
)

function New-VariableLinks($tcId) {
    $linkId = "cc76-link-$($tcId.ToLower())"
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc76-var-$($col.ToLower())"
        $sb += "   <variableLinks>`n      <testDataLinkId>$linkId</testDataLinkId>`n      <type>DATA_COLUMN</type>`n      <value>$col</value>`n      <variableId>$varId</variableId>`n   </variableLinks>"
    }
    return $sb -join ''
}

function New-Variables {
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc76-var-$($col.ToLower())"
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
// CSV: ChargeCreation-TestCases.csv | CCIPH-76 | $tcId

def cc = new CC()
cc.executeDtcCalculationTest(tcId, [
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
    $guid = "cc76-$($tcId.ToLower())-0001"
    $linkId = "cc76-link-$($tcId.ToLower())"

    $tcXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>$desc (CCIPH-76 / $tcId)</description>
   <name>$tcId</name>
   <tag>GenAI,Critical,CCIPH-76,ChargeCreation,DTC</tag>
   <comment>CSV: ChargeCreation-TestCases.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-76 | $tcId</comment>
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
      <testDataId>Data Files/ChargeCreation_CCIPH76_Data</testDataId>
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

Write-Host "Done. Generated $($tests.Count) test cases under CCIPH-76."
