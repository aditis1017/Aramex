import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-123 | TC_AR_FUNC_044 | AC2 — Export file column headers

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndServiceChargeSettings(search_Criteria, search_By, search_Value, entity)
//ar_management.clickArServiceChargeSettingsAddNew()
ar_management.toggleArSpecialServiceChargeSettings()
long exportStart = System.currentTimeMillis()
ar_management.clickArServiceChargeSettingsExportToExcel()
String exportPath = ar_management.verifyArServiceChargeSettingsExportDownloaded(exportStart)
ar_management.verifyArServiceChargeExportFileContainsColumnHeaders(exportPath, service_Columns)

WebUI.takeScreenshot()
WebUI.closeBrowser()
