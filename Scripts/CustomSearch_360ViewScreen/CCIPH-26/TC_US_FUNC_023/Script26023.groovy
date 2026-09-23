import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-26 | TC_US_FUNC_023 | Verify all required columns visible in defined order

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchWithFiltersAndOptionalCriteria(charge_Group, charge_Code, charge_Status)
cs.verifyResultGridColumnHeadersInOrder()
WebUI.takeScreenshot()
WebUI.closeBrowser()
