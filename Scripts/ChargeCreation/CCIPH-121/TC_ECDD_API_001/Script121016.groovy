import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_ECDD_API_001'
// CSV: ChargeCreation-TestCases.csv | CCIPH-121 | TC_ECDD_API_001

def cc = new CC()
cc.executeEcdDdoTest(tcId, [
	tracking_Number : tracking_Number,
	shipper_Number : shipper_Number,
	ineligible_Tracking_Number : ineligible_Tracking_Number,
	entity : entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	expected_Status : expected_Status,
	flow_Type : flow_Type,
	login_Required : login_Required,
	estimate_File_Path : estimate_File_Path,
	upload_Count : upload_Count,
	rerun_Count : rerun_Count,
	batch_Eligible_Count : batch_Eligible_Count,
	skip_Reason : skip_Reason
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}