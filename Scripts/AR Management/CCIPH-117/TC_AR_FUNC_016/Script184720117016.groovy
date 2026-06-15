import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-117 | TC_AR_FUNC_016 | AC1

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.performArSearch(search_Criteria, search_By, search_Value, entity)
ap.verifyArSearchResultsGridColumnHeaders()
ap.verifyArAccountNumberCellsAreHyperlinks()

WebUI.takeScreenshot()
WebUI.closeBrowser()
