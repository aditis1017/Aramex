import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_003 | Verify Charge Group controls available Charge Codes

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()

cs.selectChargeGroupValue('Cost')
cs.validateDropdownValues([
	'ccfe-customs-charge-code': ['ECD', 'ECS', 'EC3']
])

cs.selectChargeGroupValue('Revenue')
cs.validateDropdownValues([
	'ccfe-customs-charge-code': ['DTA', 'DTC', 'DTS', 'DT3']
])

WebUI.takeScreenshot()
WebUI.closeBrowser()
