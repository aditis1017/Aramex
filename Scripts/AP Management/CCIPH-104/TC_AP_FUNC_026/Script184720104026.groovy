import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-104 | TC_AP_FUNC_026 | AC1

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openApAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ap_management.verifyApAccountDetailPageLoaded()
WebUI.takeScreenshot()
WebUI.closeBrowser()
