import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-117 | TC_AR_FUNC_017 | AC3

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.performArSearch(search_Criteria, search_By, search_Value, entity)
ar_management.verifySearchResultsMatchCriteria(search_Criteria, search_Value)
ar_management.logManualBackendDataVerificationReminder()

WebUI.takeScreenshot()
WebUI.closeBrowser()
