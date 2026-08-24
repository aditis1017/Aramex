import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_Detailed_29
// Steps: Login → Customs Search → Search (no tracking number) → Shipment row → History → verify 50/page

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchAndOpenFirstResultWithoutTracking()
cs.verifyHistoryPaginationDefaultPageSize(50)
WebUI.takeScreenshot()
WebUI.closeBrowser()
