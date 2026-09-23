import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_006

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifySearchWithMultipleTrackingNumbers(tracking_Numbers)
cs.clickMultipleButton()
cs.verifyMultipleShipmentsTextPresent()
cs.enterMultipleSearchValues(tracking_Numbers)
WebUI.delay(2)
cs.clickOkayButton()
WebUI.delay(2)
cs.clickSearch()
cs.verifySearchResultsDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()
