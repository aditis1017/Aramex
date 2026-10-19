import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-549 | ID: BOE_004
// Jira: https://aramex.atlassian.net/browse/CCIPH-549
// Set awb_ShipmentA and awb_ShipmentB. Declaration Number must be globally unique.

def login = new MSLogin()
def fpr = new FPR()

Map<String, String> testData = [
	awb_ShipmentA  : '',
	awb_ShipmentB  : '',
	entity_Name    : 'RUH',
	decl_Duplicate : 'DEC001'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

'BOE_004: duplicate Declaration Number across different shipments is rejected'
fpr.executeBoe004CrossShipmentDuplicateRejectedFlow(testData)

WebUI.takeScreenshot()
//WebUI.closeBrowser()
