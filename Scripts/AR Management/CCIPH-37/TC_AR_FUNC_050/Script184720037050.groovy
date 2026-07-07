import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

//  CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_050 | AC4 — Reset after filter clears all filters

def login = new MSLogin()
def ar_management = new AR()
def rs_Type_Value = String.valueOf(System.currentTimeMillis() % 100000)

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetAddNew()
WebUI.delay(1)
ar_management.fillArRatesheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value)
//ar_management.clickArRatesheetAddRatePlus()
ar_management.clickArRatesheetSaveRow()
ar_management.verifyArRatesheetRateValuePresent(rs_Type_Value)
ar_management.openArManagementHistoryFromLanding()
ar_management.clickArHistoryDateFilter(history_Filter)
ar_management.verifyArHistoryTableStillDisplayed()
ar_management.clickArHistoryResetFilters()
ar_management.verifyArHistoryFiltersCleared()
ar_management.clickBackHistory()
ar_management.clickFirstArAccountNumberLink()
ar_management.toggleArSpecialRateSheetSettings()
ar_management.verifyArRatesheetSettingsPanelVisible()
ar_management.verifyArRatesheetRateValuePresent(rs_Type_Value)
ar_management.clickArRatesheetDeleteRowByDescription(rs_Type_Value)
ar_management.confirmArRatesheetDeletePopup()
ar_management.verifyArRatesheetRateValueNotPresent(rs_Type_Value)
WebUI.takeScreenshot()
WebUI.closeBrowser()
