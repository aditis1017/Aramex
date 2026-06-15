import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management-Data.csv | CCIPH-200 | TC_AP_FUNC_040 | AC1 — mandatory validation on Save

def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.clickCreateSaveWithoutMandatoryFields()
ap_management.verifyCreateApMandatoryValidation()
WebUI.takeScreenshot()
WebUI.closeBrowser()
