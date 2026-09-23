import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_DROPDOWN_SEARCH_003 | Verify search functionality is case-insensitive across all searchable dropdowns

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyDropdownSearchCaseInsensitive(dropdown_Label, dropdown_Select_Value, [dropdown_Case_Variant_1, dropdown_Case_Variant_2, dropdown_Case_Variant_3])
WebUI.takeScreenshot()
WebUI.closeBrowser()
