import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_013'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_013

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
sc.navigateToGlobalSettingsAndVerifyLoaded()
sc.editFirstGlobalRuleAndSave()
sc.validateCustomScreenForRule(expected_Behavior, charge_Group, charge_Code)
WebUI.takeScreenshot()
WebUI.closeBrowser()
