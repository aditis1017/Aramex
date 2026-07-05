import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_069 | AC5 — Percentage Of dropdown values

def login = new MSLogin()
def ar_management = new AR()
def rateValue = String.valueOf(System.currentTimeMillis() % 100)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
def  rs_Type_Value = String.valueOf(System.currentTimeMillis() % 1000)

'This is for Login Profile'
	login.loginToCustomsManagementUsingAutomationProfile()
	'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetAddNew()
//ar_management.verifyArRatesheetPercentOfDropdownOptions(rs_Rate_Type)
ar_management.fillArRatesheetNewRowFromDataWithRateDetails(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value, rs_Percent_Of)
ar_management.clickArRatesheetSaveRow()
ar_management.verifyArRatesheetRateTypePresent(rs_Rate_Type)
ar_management.clickArRatesheetDeleteRowByDescription(rs_Type_Value)
ar_management.confirmArRatesheetDeletePopup()
ar_management.verifyArRatesheetRateValueNotPresent(rs_Type_Value)
WebUI.takeScreenshot()
WebUI.closeBrowser()
