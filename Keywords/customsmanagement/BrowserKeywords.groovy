package customsmanagement

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions

/**
 * Shared browser actions for CCI Web UI automation (session bootstrap, hard refresh).
 */
class BrowserKeywords {

	private static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30

	private static boolean isDriverAlive() {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) {
				return false
			}
			driver.getWindowHandles()
			return true
		} catch (Exception ignored) {
			return false
		}
	}

	/**
	 * Opens Chrome if no active session exists (used before direct URL navigation).
	 */
	@Keyword
	static void ensureChromeBrowserOpen() {
		if (isDriverAlive()) {
			return
		}
		try {
			WebUI.closeBrowser()
		} catch (Exception ignored) {
		}
		WebUI.openBrowser('')
		WebUI.maximizeWindow()
	}

	/**
	 * Hard refresh (bypass cache), then wait for page load — use when manual steps say "hard refresh".
	 *
	 * @param pageLoadTimeoutSec seconds for {@link WebUI#waitForPageLoad(int)}
	 */
	@Keyword
	static void hardRefresh(int pageLoadTimeoutSec = DEFAULT_PAGE_LOAD_TIMEOUT) {
		WebDriver driver = DriverFactory.getWebDriver()
		if (driver == null) {
			KeywordUtil.markFailedAndStop('hardRefresh: no active WebDriver session')
			return
		}

		int timeout = pageLoadTimeoutSec > 0 ? pageLoadTimeoutSec : DEFAULT_PAGE_LOAD_TIMEOUT
		boolean reloaded = false

		try {
			Map<String, Object> params = new HashMap<String, Object>()
			params.put('ignoreCache', true)
			driver.executeCdpCommand('Page.reload', params)
			reloaded = true
			KeywordUtil.logInfo('hardRefresh: Page.reload (ignoreCache=true) via CDP')
		} catch (Throwable cdpEx) {
			KeywordUtil.logInfo("hardRefresh: CDP reload failed (${cdpEx.message}); trying Ctrl+Shift+R")
			try {
				new Actions(driver)
					.keyDown(Keys.CONTROL)
					.keyDown(Keys.SHIFT)
					.sendKeys('r')
					.keyUp(Keys.SHIFT)
					.keyUp(Keys.CONTROL)
					.perform()
				reloaded = true
			} catch (Throwable keysEx) {
				KeywordUtil.logInfo("hardRefresh: keyboard shortcut failed (${keysEx.message}); using WebUI.refresh()")
				WebUI.refresh()
				reloaded = true
			}
		}

		if (reloaded) {
			WebUI.waitForPageLoad(timeout)
		}
	}
}
