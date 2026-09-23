import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC

def tcId = 'TC_ST_002'
// CSV: Shipment Creation-TestCases.csv | CCIPH-45 | TC_ST_002 | AC2

def sc = new SC()
sc.executeShipmentTypeTest(tcId, [
	cmt: cmt,
	third_Party_Entity: third_Party_Entity,
	product_Type: product_Type,
	shipper_Number: shipper_Number,
	tracking_Number: tracking_Number,
	expected_Shipment_Type: expected_Shipment_Type,
	verify_No_Ar_Validation: verify_No_Ar_Validation,
	flow_Type: flow_Type,
	charge_Group: charge_Group,
	charge_Code: charge_Code,
	entity: entity
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
