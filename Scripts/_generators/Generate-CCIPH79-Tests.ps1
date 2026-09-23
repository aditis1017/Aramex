# Generates CCIPH-79 DTS/DT3 test cases and scripts from ChargeCreation-TestCases.csv filter
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
    $root = Split-Path $PSScriptRoot -Parent
    if (-not (Test-Path (Join-Path $root 'Corp.microservice.ccm-automation.prj'))) {
        $root = (Get-Location).Path
    }
}

$tcRoot = Join-Path $root 'Test Cases\ChargeCreation\CCIPH-79'
$scriptRoot = Join-Path $root 'Scripts\ChargeCreation\CCIPH-79'
$orRoot = Join-Path $root 'Object Repository\ChargeCreation\CCIPH-79'
New-Item -ItemType Directory -Force -Path $tcRoot, $scriptRoot, $orRoot | Out-Null

$columns = @(
    'tracking_Number','prepaid_Tracking_Number','account_Tracking_Number','third_Party_Tracking_Number',
    'ar_Rate_Tracking_Number','entity_Rate_Tracking_Number','prepaid_Rate_Tracking_Number','thirdParty_Rate_Tracking_Number',
    'no_Rate_Tracking_Number','fixed_Rate_Tracking_Number','percent_Rate_Tracking_Number','non_Invoiced_Tracking_Number',
    'shipper_Number','entity','origin_Entity','charge_Group','charge_Code','expected_Charge_Code','payment_Type','cmt_Value',
    'expected_Status','expected_Rate_Source','flow_Type','login_Required','estimate_File_Path',
    'custom_Duty','import_Vat','other_Charges','custom_Duty_Updated','import_Vat_Updated','other_Charges_Updated',
    'custom_Duty_Actual','rerun_Count','skip_Reason'
)

$tests = @(
    @{ Id='TC_DTS_001'; Num=79001; Desc='Verify DTS / DT3 charge record created for qualifying shipment' },
    @{ Id='TC_DTS_002'; Num=79002; Desc='Verify shipment payment type identified as Prepaid / Account' },
    @{ Id='TC_DTS_003'; Num=79003; Desc='Verify shipment payment type identified as 3rd Party' },
    @{ Id='TC_DTS_004'; Num=79004; Desc='Verify DTS charge created for Prepaid / Account shipment' },
    @{ Id='TC_DTS_005'; Num=79005; Desc='Verify DT3 charge created for 3rd Party shipment' },
    @{ Id='TC_DTS_006'; Num=79006; Desc='Verify contract rate applied for Account shipment' },
    @{ Id='TC_DTS_007'; Num=79007; Desc='Verify public rate applied when contract rate unavailable' },
    @{ Id='TC_DTS_008'; Num=79008; Desc='Verify public rate applied for Prepaid shipment' },
    @{ Id='TC_DTS_009'; Num=79009; Desc='Verify public rate applied for 3rd Party shipment' },
    @{ Id='TC_DTS_010'; Num=79010; Desc='Verify exception raised when no rate sheet available' },
    @{ Id='TC_DTS_011'; Num=79011; Desc='Verify calculation applied to all revenue elements' },
    @{ Id='TC_DTS_012'; Num=79012; Desc='Verify fixed rate calculated once during creation' },
    @{ Id='TC_DTS_013'; Num=79013; Desc='Verify fixed rate not recalculated on estimate update' },
    @{ Id='TC_DTS_014'; Num=79014; Desc='Verify fixed rate not recalculated on actual update' },
    @{ Id='TC_DTS_015'; Num=79015; Desc='Verify percentage rate calculated using identifier' },
    @{ Id='TC_DTS_016'; Num=79016; Desc='Verify percentage rate recalculated on estimate update' },
    @{ Id='TC_DTS_017'; Num=79017; Desc='Verify percentage rate recalculated on actual update' },
    @{ Id='TC_DTS_018'; Num=79018; Desc='Verify non-invoiced charges recalculated after estimate change' },
    @{ Id='TC_DTS_019'; Num=79019; Desc='Verify settled/invoiced charge uses variance logic after actual update' },
    @{ Id='TC_DTS_DB_001'; Num=79020; Desc='Validate DTS / DT3 row created in DB' },
    @{ Id='TC_DTS_DB_002'; Num=79021; Desc='Validate charge code stored correctly as DTS' },
    @{ Id='TC_DTS_DB_003'; Num=79022; Desc='Validate charge code stored correctly as DT3' },
    @{ Id='TC_DTS_DB_004'; Num=79023; Desc='Validate selected rate source stored' },
    @{ Id='TC_DTS_DB_005'; Num=79024; Desc='Validate fixed amount persisted correctly' },
    @{ Id='TC_DTS_DB_006'; Num=79025; Desc='Validate recalculated amount updated for percentage rate' },
    @{ Id='TC_DTS_DB_007'; Num=79026; Desc='Validate no duplicate charges on reprocess' },
    @{ Id='TC_DTS_DB_008'; Num=79027; Desc='Validate audit fields updated after recalculation' },
    @{ Id='TC_DTS_DB_009'; Num=79028; Desc='Validate variance record created after settlement update' },
    @{ Id='TC_DTS_UI_001'; Num=79029; Desc='Verify DTS / DT3 values on Shipment Detailed Page' },
    @{ Id='TC_DTS_UI_002'; Num=79030; Desc='Validate Shipment Detailed Page values with DB' }
)

function New-VariableLinks($tcId) {
    $linkId = "cc79-link-$($tcId.ToLower())"
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc79-var-$($col.ToLower())"
        $sb += "   <variableLinks>`n      <testDataLinkId>$linkId</testDataLinkId>`n      <type>DATA_COLUMN</type>`n      <value>$col</value>`n      <variableId>$varId</variableId>`n   </variableLinks>"
    }
    return $sb -join ''
}

function New-Variables {
    $sb = @()
    foreach ($col in $columns) {
        $varId = "cc79-var-$($col.ToLower())"
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
// CSV: ChargeCreation-TestCases.csv | CCIPH-79 | $tcId

def cc = new CC()
cc.executeDtsCalculationTest(tcId, [
$bindingBlock
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
"@
}

$guidBase = 0
foreach ($t in $tests) {
    $guidBase++
    $tcId = $t.Id
    $num = $t.Num
    $desc = $t.Desc
    $tcGuid = "cc79-$($tcId.ToLower())-$('{0:D4}' -f $guidBase)"
    $linkId = "cc79-link-$($tcId.ToLower())"

    $tcXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>$desc (CCIPH-79 / $tcId)</description>
   <name>$tcId</name>
   <tag>GenAI,Critical,CCIPH-79,ChargeCreation,DTS,DT3</tag>
   <comment>CSV: ChargeCreation-TestCases.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-79 | $tcId</comment>
   <origin>GenAI</origin>
   <recordOption>OTHER</recordOption>
   <testCaseGuid>$tcGuid</testCaseGuid>
   <testDataLinks>
      <combinationType>ONE</combinationType>
      <id>$linkId</id>
      <iterationEntity>
         <iterationType>SPECIFIC</iterationType>
         <value>1</value>
      </iterationEntity>
      <testDataId>Data Files/ChargeCreation_CCIPH79_Data</testDataId>
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
}

# OR locators
@(
@{Name='tab_DTS'; Desc='DTS charge tab on Charge Detail page'; XPath="//*[self::button or self::a or self::span][normalize-space()='DTS' or contains(normalize-space(),'DTS')]" },
@{Name='tab_DT3'; Desc='DT3 charge tab on Charge Detail page'; XPath="//*[self::button or self::a or self::span][normalize-space()='DT3' or contains(normalize-space(),'DT3')]" },
@{Name='val_dtsChargeStatus'; Desc='DTS charge status badge'; XPath="//*[contains(@class,'badge') or contains(@class,'status')][contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'DUE') or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'SETTLED')]" },
@{Name='val_dt3ChargeStatus'; Desc='DT3 charge status badge'; XPath="//*[contains(@class,'badge') or contains(@class,'status')][contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'DUE') or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'SETTLED')]" },
@{Name='val_rateSource'; Desc='DTS/DT3 rate source label'; XPath="//*[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'RATE') and contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'SOURCE')]/following::*[self::td or self::span or self::div][normalize-space()!=''][1]" },
@{Name='val_contractRate'; Desc='DTS/DT3 contract rate value'; XPath="//*[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'CONTRACT')]/following::*[self::td or self::span or self::div][normalize-space()!=''][1]" },
@{Name='section_revenueCharges'; Desc='Revenue charges section on charge detail'; XPath="//*[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'REVENUE') and contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'CHARGE')]" },
@{Name='txt_anchor_EntitySettings'; Desc='Entity Settings page anchor'; XPath="//*[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'ENTITY SETTINGS') or contains(@href,'entity-settings')]" },
@{Name='msg_calculationError'; Desc='Calculation error when rate sheet missing'; XPath="//*[contains(@class,'error') or contains(@class,'alert')][contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'RATE') or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'ERROR')]" }
) | ForEach-Object {
    $g = [guid]::NewGuid().ToString()
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>$($_.Desc)</description>
   <name>$($_.Name)</name>
   <tag></tag>
   <elementGuidId>cc79-$g</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>$($_.XPath)</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
"@
    Set-Content -Path (Join-Path $orRoot "$($_.Name).rs") -Value $xml -Encoding UTF8
}

Write-Host "Generated $($tests.Count) test cases under CCIPH-79"
