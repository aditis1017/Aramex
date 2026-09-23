import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_010

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyCombinedFilterSearch()
cs.clickMultipleButton()
cs.verifyMultipleShipmentsTextPresent()
cs.enterMultipleSearchValues(invalid_Tracking_Numbers)
WebUI.delay(2)
cs.clickOkayButton()
WebUI.delay(2)
cs.clickSearch()
cs.verifyNoDataFoundMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
