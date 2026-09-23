import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_DROPDOWN_SEARCH_001 | Verify user can search and select a value from searchable dropdown

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchAndSelectInDropdownByLabel(dropdown_Label, dropdown_Search_Keyword, dropdown_Select_Value)
WebUI.takeScreenshot()
WebUI.closeBrowser()
