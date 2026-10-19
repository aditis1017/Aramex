import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-549 | ID: BOE_003
// Jira: https://aramex.atlassian.net/browse/CCIPH-549
// Set awb_Primary. Second Actuals upload with the same Declaration Number on the same AWB must be rejected.

def login = new MSLogin()
def fpr = new FPR()

Map<String, String> testData = [
	awb_Primary    : '',
	entity_Name    : 'RUH',
	decl_Duplicate : 'DEC001'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

'BOE_003: duplicate Declaration Number within the same shipment is rejected'
fpr.executeBoe003SameShipmentDuplicateRejectedFlow(testData)

WebUI.takeScreenshot()
WebUI.closeBrowser()
