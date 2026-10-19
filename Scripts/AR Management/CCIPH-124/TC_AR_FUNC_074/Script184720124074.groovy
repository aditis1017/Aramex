import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-124 | TC_AR_FUNC_074 | AC1 — Edit Service Code (editable Active)

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
//ar_management.toggleArSpecialServiceChargeSettings()
ar_management.verifyToggleEnabled('Special Service Charge Settings')
//ar_management.verifyArServiceChargeSettingsPanelVisible()
ar_management.editArServiceChargeActiveToggleAndCancel()

WebUI.takeScreenshot()
WebUI.closeBrowser()
