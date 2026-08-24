import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_003 | AC4/AC5 UI smoke

def login = new MSLogin()
def b2b = new B2B()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
//login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToB2BKeywordsManager()
//b2b.navigateToStageB2BKeywordsManager()
b2b.verifyPrimaryPageControlsVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
