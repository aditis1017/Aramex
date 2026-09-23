import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_037'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_037

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
sc.verifyPaymentStatusOnCustomScreen('Due,Settled,Pending calculation,Terminated,Cancelled')
WebUI.takeScreenshot()
WebUI.closeBrowser()
