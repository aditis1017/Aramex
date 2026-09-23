import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_DTC_008'
// CSV: ChargeCreation-TestCases.csv | CCIPH-76 | TC_DTC_008

def cc = new CC()
cc.executeDtcCalculationTest(tcId, [
	tracking_Number : tracking_Number,
	cash_Tracking_Number : cash_Tracking_Number,
	account_Tracking_Number : account_Tracking_Number,
	account_Fallback_Tracking_Number : account_Fallback_Tracking_Number,
	no_Rate_Tracking_Number : no_Rate_Tracking_Number,
	fixed_Rate_Tracking_Number : fixed_Rate_Tracking_Number,
	percent_Rate_Tracking_Number : percent_Rate_Tracking_Number,
	non_Invoiced_Tracking_Number : non_Invoiced_Tracking_Number,
	shipper_Number : shipper_Number,
	entity : entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	expected_Status : expected_Status,
	payment_Type : payment_Type,
	expected_Rate_Source : expected_Rate_Source,
	flow_Type : flow_Type,
	login_Required : login_Required,
	estimate_File_Path : estimate_File_Path,
	custom_Duty : custom_Duty,
	import_Vat : import_Vat,
	other_Charges : other_Charges,
	custom_Duty_Updated : custom_Duty_Updated,
	import_Vat_Updated : import_Vat_Updated,
	other_Charges_Updated : other_Charges_Updated,
	expected_Contract_Rate : expected_Contract_Rate,
	rerun_Count : rerun_Count,
	skip_Reason : skip_Reason
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
