import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_046 | AC4 — Reset clears filters

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
ar_management.openArManagementHistoryFromLanding()
ar_management.clickArHistoryDateFilter(history_Filter)
ar_management.verifyArHistoryTableStillDisplayed()
ar_management.clickArHistoryResetFilters()
ar_management.verifyArHistoryFiltersCleared()

WebUI.takeScreenshot()
WebUI.closeBrowser()
