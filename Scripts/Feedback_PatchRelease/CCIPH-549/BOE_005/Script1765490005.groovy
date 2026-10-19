import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-549 | ID: BOE_005
// Jira: https://aramex.atlassian.net/browse/CCIPH-549
// Set awb_ShipmentA and awb_ShipmentB. Unique Declaration Numbers must map on each 360 View.

def login = new MSLogin()
def fpr = new FPR()

Map<String, String> testData = [
	awb_ShipmentA  : '',
	awb_ShipmentB  : '',
	entity_Name    : 'RUH',
	decl_ShipmentA : 'DEC001',
	decl_ShipmentB : 'DEC002'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

'BOE_005: unique Declaration Numbers accepted and mapped per shipment'
fpr.executeBoe005UniqueDeclarationsAcrossShipmentsFlow(testData)

WebUI.takeScreenshot()
WebUI.closeBrowser()
