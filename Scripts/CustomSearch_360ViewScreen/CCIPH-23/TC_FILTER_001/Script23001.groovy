import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-23 | TC_FILTER_001 | Verify Hide Filters button availability and default state

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyHideFiltersButtonAvailabilityAfterSearch()
cs.enterMultipleSearchValues(tracking_Numbers)
WebUI.takeScreenshot()
WebUI.closeBrowser()
