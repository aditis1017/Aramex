import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-103 | TC_AP_FUNC_035 | AC3 — DB check is manual

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.performSearch(search_Criteria, search_By, search_Value, entity)
ap_management.verifySearchResultsTableVisible()
ap_management.verifyAccountNumberCellsAreHyperlinks()
ap_management.logManualBackendDataVerificationReminder()
WebUI.takeScreenshot()
WebUI.closeBrowser()
