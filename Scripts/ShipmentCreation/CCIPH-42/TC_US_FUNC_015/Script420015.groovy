import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_015'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_015

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
sc.createDduGlobalRule(rule_Scenario)
sc.verifyChargeCodesOnCustomScreen(expected_Charge_Codes)
WebUI.takeScreenshot()
WebUI.closeBrowser()
