import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-549 | ID: BOE_001
// Jira: https://aramex.atlassian.net/browse/CCIPH-549
// Login CSV lines are preconditions; session uses MicrosoftLoginKeywords automation profile.
// Set awb_Primary after a test shipment exists so sequential Actuals uploads can map BOE slots.

def login = new MSLogin()
def fpr = new FPR()
def cs = new CS()

Map<String, String> testData = [
	awb_Primary     : '',
	entity_Name     : 'RUH',
	decl_BOE1       : 'DEC-B1-001',
	decl_BOE2       : 'DEC-B2-002',
	decl_BOE3       : 'DEC-B3-003',
	decl_Additional : 'DEC-ADD-004'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

cs.enterTrackingNumber("3002298711114")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()

'BOE_001: upload Declaration Numbers for BOE1, BOE2, BOE3, Additional; verify mapping on 360 View'
fpr.executeBoe001FourSlotMappingFlow(testData)

WebUI.takeScreenshot()
//WebUI.closeBrowser()
