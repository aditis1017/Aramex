import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_012

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.performArSearch(search_Criteria, search_By, search_Value, entity)
//ar_management.verifyIncorrectAccountNameMessage()
ar_management.verifyNoRecordsFoundMessage()

WebUI.takeScreenshot()
WebUI.closeBrowser()
