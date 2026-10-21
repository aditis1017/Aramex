import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-669 | Story: CCIPH-662 | TC_ID: TC_APM_REG_001
// cci-katalon-from-jira | cci-automation-from-story

def login = new MSLogin()
def ap_management = new APManagement()

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.verifyCreateApAccountFieldsVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
