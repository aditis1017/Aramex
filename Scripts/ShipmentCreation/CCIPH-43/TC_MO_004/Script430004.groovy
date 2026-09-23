import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC

def tcId = 'TC_MO_004'
// CSV: Shipment Creation-TestCases.csv | CCIPH-43 | TC_MO_004

def sc = new SC()
sc.executeManualOverrideTest(tcId, [
	shipment_Type: shipment_Type,
	upload_Type: upload_Type,
	entity: entity,
	tracking_Number: tracking_Number,
	fixture_Path: fixture_Path,
	destination_Reached: destination_Reached,
	customs_Cleared: customs_Cleared,
	payment_Confirmed: payment_Confirmed,
	shipment_Status: shipment_Status,
	expected_Result: expected_Result,
	expected_Notification_Subject: expected_Notification_Subject,
	flow_Type: flow_Type,
	api_Payload: api_Payload
])
WebUI.takeScreenshot()
if (!'DB_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim()) && !'API_ONLY'.equalsIgnoreCase((flow_Type ?: '').trim())) {
	WebUI.closeBrowser()
}
