import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def tcId = 'TC_US_FUNC_001'
// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_001

def sc = new SC()
def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectDropdownValueList("charge-status", charge_Status)
cs.selectDropdownValueList("origin-entity", origin_Entity)
cs.enterTrackingNumber(tracking_Numbers)
cs.clickSearch()
cs.verifySearchResultsForKnownShipment(search_Criteria, tracking_Numbers)
//cs.selectDropdownValueList("o", origin_Entity)
//sc.verifyEligibilityCriteriaInDb(product_Group, shipment_Status)
WebUI.takeScreenshot()
