import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_005

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
//cs.verifySearchWithDateRangeOnly(date_Type, start_Date, end_Date)
cs.setCustomDate('Start Date', '18-08-2026')
cs.setCustomDate('End Date', '21-08-2026')
cs.clickSearch()
cs.verifySearchWithoutFiltersShowsData()
WebUI.takeScreenshot()
WebUI.closeBrowser()
