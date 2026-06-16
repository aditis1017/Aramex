import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-123 | TC_AR_FUNC_043 | AC2 — Export to Excel download

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndServiceChargeSettings(search_Criteria, search_By, search_Value, entity)
ap.toggleArSpecialServiceChargeSettings()
long exportStart = System.currentTimeMillis()
ap.clickArServiceChargeSettingsExportToExcel()
ap.verifyArServiceChargeSettingsExportDownloaded(exportStart)

WebUI.takeScreenshot()
WebUI.closeBrowser()
