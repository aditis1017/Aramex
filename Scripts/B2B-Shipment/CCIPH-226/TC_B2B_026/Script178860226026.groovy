import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_026 | CCIPH-476 AR Business Type matrix

// Logic: keyword match -> B2B; no consignee number -> B2C; else AR Business Type mapping.

def login = new MSLogin()
def b2b = new B2B()

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
'B2B Keywords Manager / classification steps'
b2b.verifyShipmentClassificationOutcome((shipmentNoConsigneeNumber ?: '') as String, 'B2C', 'Consignee Number not available -> B2C')
b2b.verifyShipmentClassificationOutcome((shipmentEcommerce ?: '') as String, 'B2C', 'AR Business Type E-Commerce -> B2C')
b2b.verifyShipmentClassificationOutcome((shipmentWholesale ?: '') as String, 'B2B', 'AR Business Type Wholesale -> B2B')
b2b.verifyShipmentClassificationOutcome((shipmentCorporate ?: '') as String, 'B2B', 'AR Business Type Corporate Business -> B2B')
b2b.verifyShipmentClassificationOutcome((shipmentOilGas ?: '') as String, 'B2B', 'AR Business Type Oil and Gas -> B2B')
b2b.verifyShipmentClassificationOutcome((shipmentRetailCash ?: '') as String, 'B2C', 'AR Business Type Retail (Cash) -> B2C')
WebUI.takeScreenshot()
WebUI.closeBrowser()
