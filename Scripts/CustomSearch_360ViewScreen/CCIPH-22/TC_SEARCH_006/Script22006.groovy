import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_006 | Verify search using Business Type, Account Type and Category filters

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyBusinessAccountCategorySearch(business_Type, account_Type, category)
//cs.selectDropdownValueList('acoount-type', 'Cash')
cs.selectDropdownValueList("account-type", account_Type)
cs.selectDropdownValueList("business-type", business_Type)
cs.selectDropdownValueList("category", category)
cs.clickSearch()
cs.verifySearchResultsDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()
