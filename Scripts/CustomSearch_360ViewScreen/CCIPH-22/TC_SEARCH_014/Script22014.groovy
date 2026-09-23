import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_014 | Verify user can perform consecutive searches without refreshing the page

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyConsecutiveSearchesWithoutRefresh(charge_Group, charge_Code, charge_Status, 'Revenue', 'DTC', 'All')
cs.verifySearchInputForCriteria(search_Criteria, account_Number)
cs.verifySearchResultsDisplayed()
cs.verifySearchInputForCriteria(search_Criteria1, tracking_Numbers)
cs.verifySearchResultsDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()
