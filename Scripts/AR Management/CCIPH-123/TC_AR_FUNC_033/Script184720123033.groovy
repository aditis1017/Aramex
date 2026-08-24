import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-123 | TC_AR_FUNC_033 | AC1 — Service Charge Settings columns

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndServiceChargeSettings(search_Criteria, search_By, search_Value, entity)
ar_management.toggleArSpecialServiceChargeSettings()
ar_management.verifyArServiceChargeSettingsTableColumnsFromData(service_Columns)
WebUI.takeScreenshot()
WebUI.closeBrowser()
