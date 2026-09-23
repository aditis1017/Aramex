import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-23 | TC_FILTER_003 | Verify filter section expands using Show Filters

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.clickSearch()
cs.verifyFilterSectionExpandsUsingShowFilters()
WebUI.takeScreenshot()
WebUI.closeBrowser()
