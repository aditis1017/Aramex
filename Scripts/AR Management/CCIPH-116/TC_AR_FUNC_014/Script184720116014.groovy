import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_014

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.configureSearch(search_Criteria, search_By, entity)
ap.enterSearchText("     ${search_Value}     ")
ap.clickSearch()
ap.verifyTrimmedSearchMatches("     ${search_Value}     ", validate_Value)

WebUI.takeScreenshot()
WebUI.closeBrowser()
