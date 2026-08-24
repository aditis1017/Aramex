import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
//cs.verifySearchWithSingleFilter(charge_Group, charge_Code)
cs.selectChargeGroupAndCode(charge_Group, charge_Code)
WebUI.takeScreenshot()
WebUI.closeBrowser()
