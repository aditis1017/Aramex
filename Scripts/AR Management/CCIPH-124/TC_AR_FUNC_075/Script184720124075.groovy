import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-124 | TC_AR_FUNC_075 | AC2 — Save updated service code configuration

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
ap.toggleArSpecialServiceChargeSettings()
ap.verifyArServiceChargeSettingsPanelVisible()
ap.editArServiceChargeActiveSave()

WebUI.takeScreenshot()
WebUI.closeBrowser()
