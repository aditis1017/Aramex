import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_072 | AC4 — Edit newly created RateSheet

def login = new MSLogin()
def ap = new AP()
def rs_Type_Value = String.valueOf(System.currentTimeMillis() % 1000)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
def updatedRateValue = String.valueOf(System.currentTimeMillis() % 1000 + 1 )
	
'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
//ap.createAndSaveArRatesheetRowByRateType(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ap.clickArRatesheetAddNew()
ap.fillArRatesheetNewRowFromData(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ap.clickArRatesheetSaveRow()
ap.verifyArRatesheetRowPresent(description, rs_Rate_Type)
WebUI.delay(2)
ap.clickArRatesheetEditDescriptionRow(description)
//ap.updateArRatesheetRate1Value(updatedRateValue)
ap.updateArRatesheetRate1Value(description, updatedRateValue)
ap.clickSaveEditRatesheet()
ap.verifyArRatesheetRowPresent(description, updatedRateValue)

WebUI.takeScreenshot()
WebUI.closeBrowser()
