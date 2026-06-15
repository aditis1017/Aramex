import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

//  CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_050 | AC4 — Reset after filter clears all filters

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndServiceChargePanel(search_Criteria, search_By, search_Value, entity)
ap.openArManagementHistoryFromLanding()
ap.clickArHistoryDateFilter(history_Filter)
ap.verifyArHistoryTableStillDisplayed()
ap.clickArHistoryResetFilters()
ap.verifyArHistoryFiltersCleared()

WebUI.takeScreenshot()
WebUI.closeBrowser()
