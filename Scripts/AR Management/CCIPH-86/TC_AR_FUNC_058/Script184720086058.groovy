import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_058 — Create row then delete (same pattern as TC_AR_FUNC_057)

def login = new MSLogin()
def ar_management = new AR()
def rs_Type_Value = String.valueOf(System.currentTimeMillis() % 100000)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
	
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)



ar_management.clickArRatesheetAddNew()
ar_management.fillArRatesheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ar_management.clickArRatesheetSaveRow()



ar_management.verifyArRatesheetRateValuePresent(rs_Type_Value)
ar_management.clickArRatesheetDeleteRowByDescription(rs_Type_Value)
ar_management.confirmArRatesheetDeletePopup()

ar_management.verifyArRatesheetRateValueNotPresent(rs_Type_Value)
WebUI.takeScreenshot()
WebUI.closeBrowser()
