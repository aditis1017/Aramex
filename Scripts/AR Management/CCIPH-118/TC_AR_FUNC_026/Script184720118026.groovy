import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-118 | TC_AR_FUNC_026 | AC1 / AC2

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
login.loginToCustomsManagementUsingProfile()
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ap.verifyArDtaPageLoaded()
ap.verifyArAccountDetailFieldsVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
