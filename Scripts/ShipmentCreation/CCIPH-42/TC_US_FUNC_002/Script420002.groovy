import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_002'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_002

def sc = new SC()
//sc.verifyEligibilityCriteriaInDb(product_Group, shipment_Status, account_Number)
WebUI.takeScreenshot()
