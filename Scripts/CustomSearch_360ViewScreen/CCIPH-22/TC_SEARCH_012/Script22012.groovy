import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_012 | Verify search results are accurate with database records

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifySearchResultsForKnownShipment(search_Criteria, tracking_Numbers)
WebUI.takeScreenshot()
WebUI.closeBrowser()
