import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_047 | AC3 — Export download

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
ap.openArManagementHistoryFromLanding()
ap.clickArHistoryExport()
String exportPath = ap.verifyLatestArManagementHistoryExportDownloaded(exportStart, null)
KeywordUtil.logInfo("AR History export: ${ap.getLastArHistoryExportFileName()} at ${exportPath}")

WebUI.takeScreenshot()
WebUI.closeBrowser()
