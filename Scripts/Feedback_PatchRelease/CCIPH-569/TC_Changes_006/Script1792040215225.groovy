import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import ChargeCreation.BusinessActions as CC
// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_009

def login = new MSLogin()
def ar_management = new AR()
def cs = new CS()
def charge = new CC()

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()

//cs.navigateToCustomsSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.verifySearchInputForCriteria('Tracking Number', '3002298711114')
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()

cs.verifyFieldValue("Shipment Business Type", 'B2C')

WebUI.takeScreenshot()
WebUI.closeBrowser()