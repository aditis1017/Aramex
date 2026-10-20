import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-660 | Story: CCIPH-652 | TC_ID: TC_APM_SEC_001
// cci-katalon-from-jira | cci-automation-from-story
// Partial automation: admin profile smoke. Full RBAC matrix remains manual per CCIPH-660.

def login = new MSLogin()
def ap_management = new APManagement()

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.verifyCreateApAccountPageLoaded()
KeywordUtil.logInfo('CCIPH-660: Customs Manager and unauthorized-role checks require separate credentials — complete manually in Jira.')
WebUI.takeScreenshot()
WebUI.closeBrowser()
