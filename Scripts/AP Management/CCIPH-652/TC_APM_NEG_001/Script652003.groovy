import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-655 | Story: CCIPH-652 | TC_ID: TC_APM_NEG_001
// cci-katalon-from-jira | cci-automation-from-story

def login = new MSLogin()
def ap_management = new APManagement()

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.clickCreateSaveWithoutMandatoryFields()
ap_management.verifyCreateApMandatoryValidation()
WebUI.takeScreenshot()
WebUI.closeBrowser()
