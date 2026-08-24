import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_048 | AC3 — Export file column headers

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
ar_management.openArManagementHistoryFromLanding()
String downloadsDir = ar_management.getArHistoryExportDownloadsDirectory()
ar_management.clickArHistoryExport()
String path = ar_management.verifyLatestArManagementHistoryExportDownloaded(exportStart, downloadsDir)
ar_management.verifyExportedArHistoryHasColumnHeaders(path)
ar_management.deleteLastArHistoryExportFile(path)

WebUI.takeScreenshot()
WebUI.closeBrowser()
