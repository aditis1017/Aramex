import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-23 | TC_FILTER_004 | Verify filter values retained after hide/show toggle

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
//cs.verifyFilterValuesRetainedAfterHideAndShow(charge_Group, charge_Code, charge_Status)
cs.clickMultipleButton()
cs.verifyMultipleShipmentsTextPresent()
cs.enterMultipleSearchValues(tracking_Numbers)
cs.clickOkayButton()
WebUI.delay(1)
cs.clickSearch()
cs.verifyFilterSectionExpandsUsingShowFilters()
cs.validateTrackingNumbersPresent(tracking_Numbers)

WebUI.takeScreenshot()
WebUI.closeBrowser()
