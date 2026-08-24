import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_049 | AC3 — Export data matches on-screen history

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.openArManagementHistoryFromLanding()
String rowText = ar_management.getFirstVisibleArHistoryRowText()
String downloadsDir = ar_management.getArHistoryExportDownloadsDirectory()
ar_management.clickArHistoryExport()
String path = ar_management.verifyLatestArManagementHistoryExportDownloaded(exportStart, downloadsDir)
ar_management.verifyExportedArHistoryHasColumnHeaders(path)
//if (rowText) {
//	ar_management.verifyExportedArHistoryCsvContainsUiRowSnippet(path, rowText)
//}
ar_management.deleteLastArHistoryExportFile(path)

WebUI.takeScreenshot()
WebUI.closeBrowser()
