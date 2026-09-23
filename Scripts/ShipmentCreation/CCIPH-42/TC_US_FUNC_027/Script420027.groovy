import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_US_FUNC_027'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_027

def sc = new SC()
sc.verifyAccountStatusAndOutstanding(charge_Code, 'Blocked', 'Entity Management')
WebUI.takeScreenshot()
