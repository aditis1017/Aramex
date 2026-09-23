import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_039'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_039

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
sc.verifyPaymentStatusOnCustomScreen(expected_Status)
WebUI.takeScreenshot()
WebUI.closeBrowser()
