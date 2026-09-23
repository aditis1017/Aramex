import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Custom Search + 360 View Screen.csv | CCIPH-488 | TC_US_FUNC_053

def cs = new CS()
def login = new MSLogin()

String chargeGroup = 'Revenue'
String chargeCode = 'DTS'
String chargeStatus = 'All'
String trackingNumber = ''
String expectedCurrency = ''
String chargeCodes = ''
String expectedProductGroup = ''
String expectedBusinessType = ''
try {
	if (binding.hasVariable('charge_Group') && charge_Group) { chargeGroup = charge_Group.toString().trim() ?: chargeGroup }
	if (binding.hasVariable('charge_Code') && charge_Code) { chargeCode = charge_Code.toString().trim() ?: chargeCode }
	if (binding.hasVariable('charge_Status') && charge_Status) { chargeStatus = charge_Status.toString().trim() ?: chargeStatus }
	if (binding.hasVariable('tracking_Number') && tracking_Number) { trackingNumber = tracking_Number.toString().trim() }
	if (binding.hasVariable('expected_Currency') && expected_Currency) { expectedCurrency = expected_Currency.toString().trim() ?: expectedCurrency }
	if (binding.hasVariable('charge_Codes') && charge_Codes) { chargeCodes = charge_Codes.toString().trim() ?: chargeCodes }
	if (binding.hasVariable('expected_Product_Group') && expected_Product_Group) { expectedProductGroup = expected_Product_Group.toString().trim() }
	if (binding.hasVariable('expected_Business_Type') && expected_Business_Type) { expectedBusinessType = expected_Business_Type.toString().trim() }
} catch (Exception ignored) {
	KeywordUtil.logInfo('Using hardcoded defaults for TC_US_FUNC_053')
}

KeywordUtil.logInfo('TC_US_FUNC_053 filters: Group=' + chargeGroup + ', Code=' + chargeCode + ', Codes=' + chargeCodes)

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyProductGroupAndBusinessTypeRightOfSource(chargeGroup, chargeCode, chargeStatus, trackingNumber, expectedProductGroup, expectedBusinessType)
WebUI.takeScreenshot()
WebUI.closeBrowser()