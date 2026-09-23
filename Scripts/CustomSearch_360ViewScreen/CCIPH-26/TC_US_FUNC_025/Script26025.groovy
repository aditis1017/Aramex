import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-26 | TC_US_FUNC_025 | Verify Tracking Number column values

def cs = new CS()
def login = new MSLogin()

def trackingValue = (tracking_Numbers ?: account_Number ?: search_Value ?: '').trim()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.searchWithFiltersAndOptionalCriteria(charge_Group, charge_Code, charge_Status, search_Criteria ?: 'Tracking Number', trackingValue)
cs.verifyTrackingNumberColumnValues(trackingValue)
WebUI.takeScreenshot()
WebUI.closeBrowser()
