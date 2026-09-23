import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_009 | Verify Additional Details filters functionality

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyAdditionalDetailsSearch(origin_Entity, origin_Country, dest_Entity, dest_Country, third_Party_Entity, third_Party_Country)
cs.setAdditionalDetailsToggle(true)
cs.selectDropdownValueList("origin-country", origin_Country)
cs.selectDropdownValueList("origin-entity", origin_Entity)
cs.selectDropdownValueList("dest-country", dest_Country)
cs.selectDropdownValueList("dest-entity", dest_Entity)
cs.selectDropdownValueList("third-country", third_Party_Country)
cs.selectDropdownValueList("third-entity", third_Party_Entity)
cs.clickSearch()
cs.verifySearchResultsDisplayed()

WebUI.takeScreenshot()
WebUI.closeBrowser()
