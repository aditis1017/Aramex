import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_CURR_007'
// CSV: ChargeCreation-TestCases.csv | CCIPH-74 | TC_CURR_007

def cc = new CC()
cc.executeEcdTest(tcId, [
	tracking_Number : tracking_Number,
	shipper_Number : shipper_Number,
	entity : entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	expected_Status : expected_Status,
	flow_Type : flow_Type,
	login_Required : login_Required,
	estimate_File_Path : estimate_File_Path,
	estimate_File_Path_B : estimate_File_Path_B,
	custom_Duty : custom_Duty,
	import_Vat : import_Vat,
	other_Charges : other_Charges,
	custom_Duty_Updated : custom_Duty_Updated,
	import_Vat_Updated : import_Vat_Updated,
	other_Charges_Updated : other_Charges_Updated,
	cost_Of_Goods : cost_Of_Goods,
	exchange_Rate : exchange_Rate,
	expected_Converted_Amount : expected_Converted_Amount,
	invalid_Estimate_File_Path : invalid_Estimate_File_Path,
	skip_Reason : skip_Reason
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}