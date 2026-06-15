import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-102 | TC_AP_FUNC_003 | AC2 — blank input validation

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.clickSearchWithoutInput()
ap_management.verifyMandatoryValidationMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
