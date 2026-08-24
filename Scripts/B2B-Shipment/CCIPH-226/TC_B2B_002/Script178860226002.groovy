import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_002 | AC4 navigation

def login = new MSLogin()
def b2b = new B2B()

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
b2b.verifyB2BKeywordsManagerPageLoaded()
WebUI.verifyElementVisible(findTestObject('B2B-Shipment/CCIPH-226/section_UploadExcel'), FailureHandling.STOP_ON_FAILURE)
WebUI.takeScreenshot()
WebUI.closeBrowser()
