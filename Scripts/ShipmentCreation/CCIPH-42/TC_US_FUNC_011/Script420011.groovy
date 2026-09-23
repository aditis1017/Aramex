import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_011'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_011

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Global Settings and edit rule'
sc.navigateToGlobalSettingsAndVerifyLoaded()
sc.editFirstGlobalRuleAndSave()
sc.validateCustomScreenForRule(expected_Behavior)
WebUI.takeScreenshot()
WebUI.closeBrowser()
