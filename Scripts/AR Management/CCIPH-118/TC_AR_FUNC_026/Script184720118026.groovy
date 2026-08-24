import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-118 | TC_AR_FUNC_026 | AC1 / AC2

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
login.loginToCustomsManagementUsingAutomationProfile()
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ar_management.verifyArDtaPageLoaded()
ar_management.verifyArAccountDetailFieldsVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
