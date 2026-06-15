import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_069 | AC5 — Percentage Of dropdown values

def login = new MSLogin()
def ap = new AP()
def rateValue = String.valueOf(System.currentTimeMillis() % 100)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
def  rs_Type_Value = String.valueOf(System.currentTimeMillis() % 100)

'This is for Login Profile'
	login.loginToCustomsManagementUsingProfile()
	'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ap.clickArRatesheetAddNew()
//ap.verifyArRatesheetPercentOfDropdownOptions(rateType)
ap.fillArRatesheetNewRowFromData(description, serviceDescription, rs_Shipment_Type, rateType, percentOf, rateValue)
ap.clickArRatesheetSaveRow()
//ap.verifyArRatesheetRowPresent(description)
ap.verifyArRatesheetRowPresent(description, rs_Rate_Type)

WebUI.takeScreenshot()
WebUI.closeBrowser()
