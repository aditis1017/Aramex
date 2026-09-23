import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-26 | TC_US_FUNC_028 | Verify Charge Group and Charge Code columns

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchWithFiltersAndOptionalCriteria(charge_Group, charge_Code, charge_Status)
cs.verifyChargeGroupAndChargeCodeColumns(charge_Group, charge_Code)
WebUI.takeScreenshot()
WebUI.closeBrowser()
