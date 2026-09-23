import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_042

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyMultipleSearchValid(search_Criteria, tracking_Numbers)
cs.verifySearchInputForCriteria(search_Criteria, tracking_Numbers)
cs.verifySearchResultsDisplayed()
//cs.verifySearchAndReset()
WebUI.takeScreenshot()
WebUI.closeBrowser()
