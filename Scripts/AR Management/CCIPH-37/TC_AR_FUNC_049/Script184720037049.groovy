import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_049 | AC3 — Export data matches on-screen history

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.openArManagementHistoryFromLanding()
String rowText = ap.getFirstVisibleArHistoryRowText()
ap.clickArHistoryExport()
String path = ap.verifyLatestArManagementHistoryExportDownloaded(exportStart, null)
ap.verifyExportedArHistoryCsvHasColumnHeaders(path)
if (rowText) {
	ap.verifyExportedArHistoryCsvContainsUiRowSnippet(path, rowText)
}

WebUI.takeScreenshot()
WebUI.closeBrowser()
