import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.selectMultipleFilters(charge_Group, charge_Code, charge_Status)
cs.clickSearch()
cs.verifyFirstRowChargeGroupCodeAndStatus('COST AP', charge_Code, 'DUE')
//cs.verifySearchWithMultipleFilters(charge_Group, charge_Code, charge_Status)
WebUI.takeScreenshot()
WebUI.closeBrowser()
