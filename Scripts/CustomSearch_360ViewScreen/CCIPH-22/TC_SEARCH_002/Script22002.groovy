import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_002 | Verify search using a valid combination of basic filters

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectMultipleFilters(charge_Group, charge_Code, charge_Status)
cs.clickSearch()
cs.verifySearchResultsDisplayed()
//cs.verifyBasicFiltersSearch(charge_Country, entity, charge_Group, charge_Code, charge_Status)

//cs.selectBasicFilters(charge_Country, entity, charge_Group, charge_Code, charge_Status)
WebUI.takeScreenshot()
WebUI.closeBrowser()
