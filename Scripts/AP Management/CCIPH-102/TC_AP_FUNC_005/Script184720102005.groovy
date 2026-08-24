import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.ApCciph102Data as ApData
import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-102 | TC_AP_FUNC_005 | AC9 — trim input

def tcId = 'TC_AP_FUNC_005'
def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.performSearch(search_Criteria, search_By, search_Value, entity)
ap_management.verifyTrimmedSearchMatches(search_Value, validate_Value)
WebUI.takeScreenshot()
WebUI.closeBrowser()
