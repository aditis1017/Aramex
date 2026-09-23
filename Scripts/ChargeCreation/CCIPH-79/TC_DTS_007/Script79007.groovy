import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_DTS_007'
// CSV: ChargeCreation-TestCases.csv | CCIPH-79 | TC_DTS_007

def cc = new CC()
cc.executeDtsCalculationTest(tcId, [
	tracking_Number : tracking_Number,
	prepaid_Tracking_Number : prepaid_Tracking_Number,
	account_Tracking_Number : account_Tracking_Number,
	third_Party_Tracking_Number : third_Party_Tracking_Number,
	ar_Rate_Tracking_Number : ar_Rate_Tracking_Number,
	entity_Rate_Tracking_Number : entity_Rate_Tracking_Number,
	prepaid_Rate_Tracking_Number : prepaid_Rate_Tracking_Number,
	thirdParty_Rate_Tracking_Number : thirdParty_Rate_Tracking_Number,
	no_Rate_Tracking_Number : no_Rate_Tracking_Number,
	fixed_Rate_Tracking_Number : fixed_Rate_Tracking_Number,
	percent_Rate_Tracking_Number : percent_Rate_Tracking_Number,
	non_Invoiced_Tracking_Number : non_Invoiced_Tracking_Number,
	shipper_Number : shipper_Number,
	entity : entity,
	origin_Entity : origin_Entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	expected_Charge_Code : expected_Charge_Code,
	payment_Type : payment_Type,
	cmt_Value : cmt_Value,
	expected_Status : expected_Status,
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
	custom_Duty_Actual : custom_Duty_Actual,
	rerun_Count : rerun_Count,
	skip_Reason : skip_Reason
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
