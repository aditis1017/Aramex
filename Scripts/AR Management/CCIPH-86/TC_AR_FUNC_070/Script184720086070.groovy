import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_070 | AC4 — Edit existing RateSheet row

def login = new MSLogin()
def ar_management = new AR()
def  rs_Type_Value = String.valueOf(System.currentTimeMillis() % 1000)
	
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetAddNew()
//ar_management.verifyArRatesheetPercentOfDropdownOptions(rs_Rate_Type)
ar_management.fillArRatesheetNewRowFromDataWithRateDetails(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
ar_management.clickArRatesheetSaveRow()
ar_management.clickArRatesheetEditFirstRow()
ar_management.updateArRatesheetRate1Value(AR.generateArRatesheetRandomNumber())
ar_management.clickSaveEditRatesheet()
ar_management.verifyArRatesheetSettingsPanelVisible()
ar_management.clickArRatesheetDeleteRowByDescription(serviceCode)
ar_management.confirmArRatesheetDeletePopup()
ar_management.verifyArRatesheetRateValueNotPresent(serviceCode)

WebUI.takeScreenshot()
WebUI.closeBrowser()
