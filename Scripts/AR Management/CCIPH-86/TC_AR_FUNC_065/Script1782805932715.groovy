import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_065 — max 3 rate slots; + hidden after Rate 3

def login = new MSLogin()
def ar_management = new AR()
def rs_Type_Value1 = String.valueOf(System.currentTimeMillis() % 100000)
def rs_Type_Value2 = String.valueOf(System.currentTimeMillis() % 100000 +1 )
def rs_Type_Value3 = String.valueOf(System.currentTimeMillis() % 100000 +2 )
	
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetAddNew()
ar_management.fillArRatesheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value1)
WebUI.delay(1)
ar_management.clickArRatesheetAddRatePlus()
ar_management.verifyArRatesheetRateSlotVisible(2)
ar_management.fillArRatesheetRateAndClickAddAnotherRate(2, rs_Rate_Type, rs_Type_Value2)
WebUI.delay(1)
ar_management.verifyArRatesheetRateSlotVisible(3)
ar_management.fillArRatesheetRateSlot(3, rs_Rate_Type, rs_Type_Value3)
ar_management.verifyArRatesheetAddRatePlusNotVisible()
//ar_management.clickArRatesheetDeleteRowByDescription(rs_Type_Value1)
//ar_management.confirmArRatesheetDeletePopup()
//ar_management.verifyArRatesheetRateValueNotPresent(rs_Type_Value1)

WebUI.takeScreenshot()
WebUI.closeBrowser()
