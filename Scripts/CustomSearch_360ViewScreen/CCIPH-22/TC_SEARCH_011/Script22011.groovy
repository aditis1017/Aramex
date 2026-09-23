import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_011 | Verify system displays No records found when no matching data exists

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyNoRecordsFoundForSearchValue(search_Criteria, no_Match_Search_Value ?: search_Value)
cs.enterTrackingNumber(tracking_Numbers)
cs.clickSearch()
cs.verifyNoDataFoundMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
