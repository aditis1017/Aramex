import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-26 | TC_US_FUNC_020 | Verify each row represents one shipment charge record

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchWithFiltersAndOptionalCriteria(charge_Group, charge_Code, charge_Status)
cs.verifyEachResultRowRepresentsShipmentCharge()
WebUI.takeScreenshot()
WebUI.closeBrowser()
