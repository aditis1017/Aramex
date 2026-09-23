import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_039

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.enterTrackingNumber(tracking_Numbers)
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber(tracking_Numbers)
cs.validateTrackingNumber(tracking_Numbers)
cs.clickBackButton()
WebUI.takeScreenshot()
WebUI.closeBrowser()
