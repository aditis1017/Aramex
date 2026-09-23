import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_004 | Verify search using Date Type and valid date range

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyDateTypeAndRangeSearch(date_Type, start_Date, end_Date)
WebUI.takeScreenshot()
WebUI.closeBrowser()
