import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC

def tcId = 'TC_ALLOC_002'
// CSV: Shipment Creation-TestCases.csv | CCIPH-46 | TC_ALLOC_002 | AC2

def sc = new SC()
sc.executeEntityAllocationTest(tcId, [
	shipment_Type: shipment_Type,
	destination_Entity: destination_Entity,
	third_Party_Entity: third_Party_Entity,
	cmt: cmt,
	account_Number: account_Number,
	ar_Account_Entity: ar_Account_Entity,
	tracking_Number: tracking_Number,
	expected_Account_Type: expected_Account_Type,
	expected_Account: expected_Account,
	flow_Type: flow_Type,
	charge_Group: charge_Group,
	charge_Code: charge_Code,
	entity: entity,
	product_Type: product_Type,
	shipper_Number: shipper_Number
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
