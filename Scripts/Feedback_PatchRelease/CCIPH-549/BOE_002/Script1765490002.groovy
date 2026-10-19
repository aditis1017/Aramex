import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-549 | ID: BOE_002
// Jira: https://aramex.atlassian.net/browse/CCIPH-549
// Set awb_Primary after ingestion. Expected: BOE1/BOE2/BOE3 retain respective Declaration Numbers (no swap).

def login = new MSLogin()
def fpr = new FPR()

Map<String, String> testData = [
	awb_Primary : '',
	entity_Name : 'RUH',
	decl_BOE1   : 'DEC-B1-001',
	decl_BOE2   : 'DEC-B2-002',
	decl_BOE3   : 'DEC-B3-003'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

'BOE_002: each BOE field retains its respective Declaration Number'
fpr.executeBoe002RetainRespectiveNumbersFlow(testData)

WebUI.takeScreenshot()
WebUI.closeBrowser()
