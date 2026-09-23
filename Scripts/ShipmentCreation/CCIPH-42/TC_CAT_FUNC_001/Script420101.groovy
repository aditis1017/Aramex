import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_CAT_FUNC_001'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_CAT_FUNC_001

def sc = new SC()
def login = new MSLogin()
'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Validate category rule on Global Settings and Custom Search'
sc.validateCategoryRule(category_Rule, expected_Category, [tracking_Number: tracking_Number, entity: entity])
WebUI.takeScreenshot()
WebUI.closeBrowser()
