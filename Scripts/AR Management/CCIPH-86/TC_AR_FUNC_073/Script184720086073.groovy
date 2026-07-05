import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_072 | AC4 — Edit newly created RateSheet

def login = new MSLogin()
def ar_management = new AR()
def rs_Type_Value = String.valueOf(System.currentTimeMillis() % 1000)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
def updatedRateValue = String.valueOf(System.currentTimeMillis() % 1000 + 1 )
	
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetAddNew()
ar_management.fillArRatesheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
//ar_management.clickArRatesheetRate1ValueLinkForRow(description)
//ar_management.verifyArRatesheetCreationPageVisible()
ar_management.clickArRatesheetCreationBack()
//ar_management.verifyArAccountReceivableDetailAfterRatesheetBack()
ar_management.verifyArRatesheetSettingsPanelVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
