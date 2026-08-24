import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-103 | TC_AP_FUNC_019 | AC3 (UI match to criteria)

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.performSearch(search_Criteria, search_By, search_Value, entity)
ap_management.verifySearchResultsMatchCriteria(search_Criteria, search_Value)
ap_management.logManualBackendDataVerificationReminder()
WebUI.takeScreenshot()
WebUI.closeBrowser()
