import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_022 | AC1 Exclude E-Commerce


def login = new MSLogin()
def b2b = new B2B()

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
'B2B Keywords Manager / classification steps'
String ref = (shipmentRef ?: '') as String
b2b.verifyShipmentClassificationOutcome(ref, 'B2C', 'AC1: E-Commerce BusinessType excluded from B2B even if keyword matches')
WebUI.takeScreenshot()
WebUI.closeBrowser()
