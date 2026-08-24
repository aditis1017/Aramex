import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_001

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
//cs.verifySearchWithoutFiltersShowsNoData()
cs.verifySearchWithoutFiltersShowsData()
WebUI.takeScreenshot()
WebUI.closeBrowser()
