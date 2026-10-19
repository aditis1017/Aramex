import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import Login_Orange.BusinessActions as OrangeLogin

/**
 * CCIPH-648 | TC_LOGIN_001 | Linked Jira: CCIPH-649
 * CSV ID: TC_LOGIN_001 | User Story: CCIPH-648 | Screen: OrangeHRM Login
 * DEFAULT_TIMEOUT: 30s (see BusinessActions)
 */

def login = new OrangeLogin()

try {
	login.openBrowserAndNavigateToLogin()
	login.verifyUsernameFieldDisplayed()
	login.enterUsername()
	login.enterPassword()
	login.clickLoginButton()
	login.verifyDashboardPageDisplayed()
	login.verifyDashboardNavigationVisible()
	WebUI.takeScreenshot()
} finally {
	login.closeBrowserSafely()
}
