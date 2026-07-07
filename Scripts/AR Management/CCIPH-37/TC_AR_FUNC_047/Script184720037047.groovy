import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_047 | AC3 — Export download

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
String exportPath = ar_management.verifyLatestArManagementHistoryExportDownloaded(exportStart, downloadsDir)
KeywordUtil.logInfo("AR History export: ${ar_management.getLastArHistoryExportFileName()} at ${exportPath}")
ar_management.deleteLastArHistoryExportFile(exportPath)

WebUI.takeScreenshot()
WebUI.closeBrowser()
