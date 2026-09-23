import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_DTC_DDO_002'
// CSV: ChargeCreation-TestCases.csv | CCIPH-122 | TC_DTC_DDO_002

def cc = new CC()
cc.executeDtcDdoTest(tcId, [
	tracking_Number : tracking_Number,
	shipper_Number : shipper_Number,
	ineligible_Tracking_Number : ineligible_Tracking_Number,
	invalid_Ecd_Tracking_Number : invalid_Ecd_Tracking_Number,
	multi_Ecd_Tracking_Number : multi_Ecd_Tracking_Number,
	entity : entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	expected_Status : expected_Status,
	flow_Type : flow_Type,
	login_Required : login_Required,
	estimate_File_Path : estimate_File_Path,
	upload_Count : upload_Count,
	rerun_Count : rerun_Count,
	multi_Ecd_Min_Count : multi_Ecd_Min_Count,
	batch_Eligible_Count : batch_Eligible_Count,
	skip_Reason : skip_Reason,
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
