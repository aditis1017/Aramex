import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-26 | TC_US_FUNC_022 | Verify result count matches backend data

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyResultCountDisplayedForBackendComparison(charge_Group, charge_Code, charge_Status)
WebUI.takeScreenshot()
WebUI.closeBrowser()
