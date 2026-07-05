import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// CSV: AP-Management.csv | CCIPH-104 | TC_AP_FUNC_033 | AC2 editable fields

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openApAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ap_management.verifyApAccountDetailEditableFields()
WebUI.takeScreenshot()
WebUI.closeBrowser()
