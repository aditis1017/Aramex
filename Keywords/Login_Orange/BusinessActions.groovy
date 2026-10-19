package Login_Orange

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import internal.GlobalVariable

/**
 * OrangeHRM demo login flows (CCIPH-648 / Login-Orange module).
 * Object Repository prefix: Login-Orange/CCIPH-648/
 */
class BusinessActions {

	private static final String OR_PREFIX = 'Login-Orange/CCIPH-648/'
	private static final int DEFAULT_TIMEOUT = 30

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static void failStop(String message) {
		try {
			WebUI.takeScreenshot()
		} catch (Exception ignored) {
		}
		KeywordUtil.markFailedAndStop(message)
	}

	private static void waitVisible(TestObject testObject, int timeout = DEFAULT_TIMEOUT) {
		WebUI.waitForElementVisible(testObject, timeout)
	}

	private static void waitVisibleClickable(TestObject testObject, int timeout = DEFAULT_TIMEOUT) {
		WebUI.waitForElementVisible(testObject, timeout)
		WebUI.waitForElementClickable(testObject, timeout)
	}

	private static String loginUrl() {
		String url = GlobalVariable.ORANGE_HRM_LOGIN_URL
		if (url == null || url.toString().trim().isEmpty()) {
			failStop('Profile variable ORANGE_HRM_LOGIN_URL is not set.')
		}
		return url.toString().trim()
	}

	private static String demoUsername() {
		String u = GlobalVariable.ORANGE_HRM_USERNAME
		return (u == null || u.toString().trim().isEmpty()) ? 'Admin' : u.toString().trim()
	}

	private static String demoPassword() {
		String p = GlobalVariable.ORANGE_HRM_PASSWORD
		return (p == null || p.toString().trim().isEmpty()) ? 'admin123' : p.toString().trim()
	}

	@Keyword
	def openBrowserAndNavigateToLogin() {
		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			WebUI.navigateToUrl(loginUrl())
			waitVisible(to('lbl_loginPageTitle'), DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 1 — launch login URL failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def verifyUsernameFieldDisplayed() {
		try {
			TestObject username = to('inp_username')
			waitVisibleClickable(username, DEFAULT_TIMEOUT)
			WebUI.verifyElementVisible(username, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementClickable(username, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 2 — username field verification failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def enterUsername(String username = null) {
		try {
			String value = (username == null || username.trim().isEmpty()) ? demoUsername() : username.trim()
			TestObject field = to('inp_username')
			waitVisibleClickable(field, DEFAULT_TIMEOUT)
			WebUI.clearText(field)
			WebUI.setText(field, value)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 3 — enter username failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def enterPassword(String password = null) {
		try {
			String value = (password == null || password.trim().isEmpty()) ? demoPassword() : password.trim()
			TestObject field = to('inp_password')
			waitVisibleClickable(field, DEFAULT_TIMEOUT)
			WebUI.clearText(field)
			WebUI.setText(field, value)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 4 — enter password failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def clickLoginButton() {
		try {
			TestObject btn = to('btn_login')
			waitVisibleClickable(btn, DEFAULT_TIMEOUT)
			WebUI.click(btn)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 5 — click Login failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def verifyDashboardPageDisplayed() {
		try {
			WebUI.waitForElementPresent(to('hdr_dashboard'), DEFAULT_TIMEOUT)
			waitVisible(to('hdr_dashboard'), DEFAULT_TIMEOUT)
			String currentUrl = WebUI.getUrl()
			if (!currentUrl.contains('dashboard')) {
				failStop("Step 6 — expected dashboard URL after login; actual: ${currentUrl}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 6 — dashboard verification failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def verifyDashboardNavigationVisible() {
		try {
			TestObject nav = to('nav_sideMenu')
			waitVisible(nav, DEFAULT_TIMEOUT)
			WebUI.verifyElementVisible(nav, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(to('hdr_dashboard'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Step 7 — dashboard navigation verification failed: ${e.getMessage()}")
		}
	}

	@Keyword
	def loginWithValidCredentialsAndVerifyDashboard() {
		openBrowserAndNavigateToLogin()
		verifyUsernameFieldDisplayed()
		enterUsername()
		enterPassword()
		clickLoginButton()
		verifyDashboardPageDisplayed()
		verifyDashboardNavigationVisible()
	}

	@Keyword
	def closeBrowserSafely() {
		try {
			WebUI.closeBrowser()
		} catch (Exception ignored) {
		}
	}
}
