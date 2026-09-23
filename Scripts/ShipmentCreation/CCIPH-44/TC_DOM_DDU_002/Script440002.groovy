import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC

def tcId = 'TC_DOM_DDU_002'
// CSV: Shipment Creation-TestCases.csv | CCIPH-44 | TC_DOM_DDU_002 | AC2

def sc = new SC()
sc.executeDomesticDduTest(tcId, [
	product_Group: product_Group,
	shipper_Account_Number: shipper_Account_Number,
	destination_Entity: destination_Entity,
	tracking_Number: tracking_Number,
	expected_Shipment_Type: expected_Shipment_Type,
	expect_Auto_Ddu: expect_Auto_Ddu,
	flow_Type: flow_Type,
	charge_Group: charge_Group,
	charge_Code: charge_Code
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
