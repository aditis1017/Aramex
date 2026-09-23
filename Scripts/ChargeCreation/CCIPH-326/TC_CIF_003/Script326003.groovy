import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ChargeCreation.BusinessActions as CC

def tcId = 'TC_CIF_003'
// CSV: ChargeCreation-TestCases.csv | CCIPH-326 | CIF-003

def cc = new CC()
cc.executeCifShipmentTest(tcId, [
	tracking_Number : tracking_Number,
	shipper_Number : shipper_Number,
	entity : entity,
	charge_Group : charge_Group,
	charge_Code : charge_Code,
	cost_Of_Goods : cost_Of_Goods,
	cc_Charges : cc_Charges,
	cc_Charges_Updated : cc_Charges_Updated,
	expected_Cif : expected_Cif,
	expected_Shipping_Charges : expected_Shipping_Charges,
	flow_Type : flow_Type,
	login_Required : login_Required,
	generate_Tracking : generate_Tracking,
	sh267_Variant : sh267_Variant,
	skip_Reason : skip_Reason,
])
WebUI.takeScreenshot()
if (!'QUEUE_DB'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
