import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_058 — Create row then delete (same pattern as TC_AR_FUNC_057)

def login = new MSLogin()
def ap = new AP()
def rs_Type_Value = String.valueOf(System.currentTimeMillis() % 100000)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
	
'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
//ap.createAndSaveArRatesheetRowByRateType(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)

//ap.createAndSaveArRatesheetRowByRateType(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ap.clickArRatesheetAddNew()
ap.fillArRatesheetNewRowFromData(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ap.clickArRatesheetSaveRow()
ap.verifyArRatesheetRowPresent(description, rs_Rate_Type)

ap.clickArRatesheetDeleteRowByDescription(description)
ap.confirmArRatesheetDeletePopup()
ap.verifyArRatesheetRowNotPresent(description)

WebUI.takeScreenshot()
WebUI.closeBrowser()
