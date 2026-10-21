import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-670 | Story: CCIPH-662 | TC_ID: TC_APM_SEC_001
// cci-katalon-from-jira | cci-automation-from-story
// Partial automation: admin profile smoke. Full RBAC matrix remains manual per CCIPH-670.

def login = new MSLogin()
def ap_management = new APManagement()

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.verifyCreateApAccountPageLoaded()
KeywordUtil.logInfo('CCIPH-670: Customs Manager and unauthorized-role checks require separate credentials — complete manually in Jira.')
WebUI.takeScreenshot()
WebUI.closeBrowser()
