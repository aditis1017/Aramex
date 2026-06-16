package customsmanagement

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
 * Microsoft Entra ID (Azure AD) interactive login for dev-cc Customs Management.
 *
 * <p><b>Credentials:</b> Set {@code MS_LOGIN_EMAIL}, and optionally {@code MS_LOGIN_PASSWORD} and
 * {@code MS_VERIFICATION_CODE} in the active Katalon Profile (or runtime override). Do not commit secrets.</p>
 *
 * <p><b>OTP / email code:</b> If your tenant requires a one-time code and {@code MS_VERIFICATION_CODE} is empty,
 * set {@code MS_AUTO_FETCH_OTP_FROM_EMAIL} to true and configure {@code MS_IMAP_*} / {@code MS_OTP_*} profile variables
 * to read the code from the mailbox via IMAP; otherwise the keyword waits {@code MS_MFA_WAIT_SECONDS} for manual entry.</p>
 *
 * <p><b>Aramex login landing:</b> If the app shows the intermediate &quot;Sign in with Microsoft&quot; button
 * (e.g. {@code dev.cc.aramex.com/login} or similar) before Entra, that button is clicked first, then Microsoft email/password.</p>
 *
 * <p><b>Usage:</b> Call {@link #loginToCustomsManagement()} once at the start of a suite or before feature tests,
 * then use existing navigation keywords (browser session is preserved).</p>
 */
class MicrosoftLoginKeywords {

	private static final String OR_PREFIX = 'Customs Management/Microsoft Login/'
	private static final int DEFAULT_TIMEOUT = 30

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static void failStop(String message) {
		try {
			if (isDriverAlive()) {
				WebUI.takeScreenshot()
			}
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

	private static boolean isDriverAlive() {
		try {
			def d = DriverFactory.getWebDriver()
			if (d == null) {
				return false
			}
			d.getWindowHandles()
			return true
		} catch (Exception e) {
			return false
		}
	}

	private static String currentUrlSafe() {
		try {
			return (WebUI.getUrl() ?: '').trim()
		} catch (Exception e) {
			return ''
		}
	}

	private static boolean isOnMicrosoftLoginHost() {
		String u = currentUrlSafe().toLowerCase()
		return u.contains('login.microsoftonline.com') || u.contains('login.live.com') || u.contains('microsoftonline')
	}

	private static boolean isOnAramexDevHost() {
		String u = currentUrlSafe().toLowerCase()
		return u.contains('dev-cc.aramex.com') || u.contains('dev.cc.aramex.com')
	}

	/** True when URL looks like an Aramex-hosted login / sign-in route (not the authenticated app shell). */
	private static boolean isOnAramexHostedAuthLandingUrl() {
		String u = currentUrlSafe().toLowerCase()
		return u.contains('/login') || u.contains('/signin') || u.contains('/sign-in')
	}

	/** True when current URL is clearly inside Customs / Global Settings (or related app) paths. */
	private static boolean hasAuthenticatedAppPath() {
		String u = currentUrlSafe().toLowerCase()
		return u.contains('customs-management') || u.contains('global-settings') ||
			u.contains('upload-charges') || u.contains('/customs/')
	}

	private static List<String> aramexMicrosoftSignInButtonXpaths() {
		return [
			"//button[contains(normalize-space(.),'Sign in with Microsoft')]",
			"//button[contains(@class,'login-module__') and contains(@class,'__button') and contains(.,'Microsoft')]",
			"//a[contains(normalize-space(.),'Sign in with Microsoft')]"
		]
	}

	private static List<String> microsoftStaySignedInYesXpaths() {
		return [
			"//*[@id='idSIButton9']",
			"//input[@type='submit' and @value='Yes' and contains(@class,'button_primary')]",
			"//*[contains(.,'Stay signed in')]/ancestor::div[contains(@class,'app')]//input[@type='submit' and @value='Yes']",
			"//*[contains(.,'Stay signed in')]/ancestor::div[contains(@class,'app')]//button[contains(.,'Yes')]"
		]
	}

	private static WebElement findFirstDisplayedByXpaths(WebDriver driver, List<String> xpaths) {
		if (driver == null || xpaths == null) {
			return null
		}
		for (String xp : xpaths) {
			List<WebElement> els = driver.findElements(By.xpath(xp))
			if (!els.isEmpty() && els.get(0).isDisplayed()) {
				return els.get(0)
			}
		}
		return null
	}

	/**
	 * Detects Aramex &quot;Sign in with Microsoft&quot; without {@code waitForElementVisible} (avoids WARN spam when
	 * this is polled from {@link #waitForDevCcApp} every few seconds while the button is absent).
	 */
	private static boolean isAramexSignInWithMicrosoftButtonDisplayed() {
		try {
			WebDriver d = DriverFactory.getWebDriver()
			return findFirstDisplayedByXpaths(d, aramexMicrosoftSignInButtonXpaths()) != null
		} catch (Exception ignored) {
			return false
		}
	}

	private static boolean isMicrosoftEmailFieldDisplayedQuick() {
		try {
			WebDriver d = DriverFactory.getWebDriver()
			if (d == null) {
				return false
			}
			String xp = "//input[@id='i0116' or @name='loginfmt' or (@type='email' and (contains(@aria-label,'email') or contains(@aria-label,'Email')))]"
			List<WebElement> els = d.findElements(By.xpath(xp))
			return !els.isEmpty() && els.get(0).isDisplayed()
		} catch (Exception ignored) {
			return false
		}
	}

	/**
	 * True only when a warm session really looks authenticated: same rules as {@link #isOnDevCcApp}, but after a short
	 * grace window so we do not skip while the SPA is still mounting the &quot;Sign in with Microsoft&quot; card
	 * (URL already contains customs-management / global-settings).
	 */
	private static boolean canShortCircuitWarmSessionAsAlreadySignedIn() {
		if (!isOnDevCcApp()) {
			return false
		}
		long deadline = System.currentTimeMillis() + 12000L
		while (System.currentTimeMillis() < deadline) {
			if (isAramexSignInWithMicrosoftButtonDisplayed()) {
				return false
			}
			if (isMicrosoftEmailFieldDisplayedQuick()) {
				return false
			}
			if (isOnMicrosoftLoginHost()) {
				return false
			}
			if (!hasAuthenticatedAppPath()) {
				return false
			}
			WebUI.delay(1)
		}
		return isOnDevCcApp()
	}

	/**
	 * True only when we appear to be past Microsoft login and inside the Aramex dev app (not on /login, not showing
	 * the intermediate &quot;Sign in with Microsoft&quot; card on an app URL).
	 */
	private static boolean isOnDevCcApp() {
		if (!isOnAramexDevHost()) {
			return false
		}
		if (isOnMicrosoftLoginHost()) {
			return false
		}
		if (isOnAramexHostedAuthLandingUrl()) {
			return false
		}
		// Same host as the app but still on the Aramex login card (e.g. /customs-management before Entra redirect)
		if (isAramexSignInWithMicrosoftButtonDisplayed()) {
			return false
		}
		return hasAuthenticatedAppPath()
	}

	/**
	 * Post–Microsoft SSO: browser is back on an Aramex dev host, not on the hosted login route, and the Aramex
	 * &quot;Sign in with Microsoft&quot; card is not showing. Used only by {@link #waitForDevCcApp} so we do not block
	 * forever when the return URL omits {@code customs-management} / {@code global-settings} path fragments.
	 */
	private static boolean isOnAramexHostAfterSsoForWait() {
		if (!isOnAramexDevHost()) {
			return false
		}
		if (isOnMicrosoftLoginHost()) {
			return false
		}
		if (isOnAramexHostedAuthLandingUrl()) {
			return false
		}
		if (isAramexSignInWithMicrosoftButtonDisplayed()) {
			return false
		}
		return true
	}

	/**
	 * Waits until browser URL shows dev-cc app (post-auth) or timeout.
	 */
	private static boolean waitForDevCcApp(int maxWaitSeconds) {
		long deadline = System.currentTimeMillis() + (maxWaitSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			if (isOnAramexHostAfterSsoForWait()) {
				return true
			}
			WebUI.delay(2)
		}
		return isOnAramexHostAfterSsoForWait()
	}

	private static void ensureBrowserOpen() {
		if (isDriverAlive()) {
			return
		}
		try {
			WebUI.closeBrowser()
		} catch (Exception ignored) {
		}
		// First argument is a URL, not a browser name — use '' then navigate via CUSTOMS_APP_URL.
		// Select Chrome (or other browser) in Katalon: Run > Web UI > Default / Chrome.
		WebUI.openBrowser('')
		WebUI.maximizeWindow()
		WebUI.delay(1)
	}

	/**
	 * Clicks the Aramex-hosted &quot;Sign in with Microsoft&quot; control when present (XPath/CSS hash tolerant; no Katalon wait WARN spam).
	 */
	private static void clickSignInWithMicrosoftOnAramexLoginIfShown() {
		long deadline = System.currentTimeMillis() + 15000L
		while (System.currentTimeMillis() < deadline) {
			try {
				WebDriver d = DriverFactory.getWebDriver()
				WebElement btn = findFirstDisplayedByXpaths(d, aramexMicrosoftSignInButtonXpaths())
				if (btn != null) {
					btn.click()
					WebUI.delay(2)
					WebUI.takeScreenshot()
					WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
					WebUI.delay(2)
					return
				}
			} catch (Exception ignored) {
			}
			WebUI.delay(1)
		}
	}

	private static void enterEmailAndNext(String email) {
		waitVisibleClickable(to('inp_ms_email'))
		WebUI.setText(to('inp_ms_email'), email)
		WebUI.takeScreenshot()
		waitVisibleClickable(to('btn_ms_next'))
		WebUI.click(to('btn_ms_next'))
		WebUI.delay(2)
		WebUI.takeScreenshot()
	}

	private static void enterPasswordAndSignInIfShown(String password) {
		if (!password) {
			return
		}
		if (WebUI.waitForElementVisible(to('inp_ms_password'), 12, FailureHandling.OPTIONAL)) {
			waitVisibleClickable(to('inp_ms_password'))
			WebUI.setText(to('inp_ms_password'), password)
			WebUI.takeScreenshot()
			waitVisibleClickable(to('btn_ms_signIn'))
			WebUI.click(to('btn_ms_signIn'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		}
	}

	private static void enterOtpAndVerifyIfShown(String otpPlain) {
		boolean otpVisible = WebUI.waitForElementVisible(to('inp_ms_otp'), 15, FailureHandling.OPTIONAL)
		if (!otpVisible) {
			return
		}
		String code = (otpPlain ?: '').trim()
		if (!code) {
			if ('true'.equalsIgnoreCase(ProfileGlobals.get('MS_AUTO_FETCH_OTP_FROM_EMAIL', 'false'))) {
				try {
					code = new MicrosoftEmailOtpFetcher().fetchLatestMicrosoftOtpFromImapUsingProfile()?.trim()
				} catch (Exception e) {
					KeywordUtil.markWarning("Automatic OTP fetch failed: ${e.message}. Falling back to manual wait.")
				}
			}
		}
		if (code) {
			waitVisibleClickable(to('inp_ms_otp'))
			WebUI.setText(to('inp_ms_otp'), code)
			WebUI.takeScreenshot()
			if (WebUI.waitForElementVisible(to('btn_ms_verify'), 8, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(to('btn_ms_verify'))
				WebUI.click(to('btn_ms_verify'))
			}
			WebUI.delay(2)
			WebUI.takeScreenshot()
			return
		}
		int waitSec = 120
		try {
			String ws = ProfileGlobals.get('MS_MFA_WAIT_SECONDS', '120')
			waitSec = Integer.parseInt(ws)
		} catch (NumberFormatException ignored) {
		}
		KeywordUtil.logInfo("MS_VERIFICATION_CODE is empty — enter the email/Authenticator code in the browser within ${waitSec} seconds.")
		WebUI.delay(Math.min(Math.max(waitSec, 30), 600))
		WebUI.takeScreenshot()
		if (WebUI.waitForElementVisible(to('btn_ms_verify'), 5, FailureHandling.OPTIONAL)) {
			waitVisibleClickable(to('btn_ms_verify'))
			WebUI.click(to('btn_ms_verify'))
			WebUI.delay(2)
		}
		WebUI.takeScreenshot()
	}

	private static void dismissStaySignedInIfConfigured() {
		String flag = ProfileGlobals.get('MS_CLICK_STAY_SIGNED_IN', 'true')
		if (!'true'.equalsIgnoreCase(flag)) {
			return
		}
		try {
			WebDriver d = DriverFactory.getWebDriver()
			WebElement yes = findFirstDisplayedByXpaths(d, microsoftStaySignedInYesXpaths())
			if (yes != null) {
				yes.click()
				WebUI.delay(2)
				WebUI.takeScreenshot()
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * Opens the Customs app entry URL, completes Microsoft sign-in (email, optional password, optional OTP),
	 * and waits until {@code dev-cc.aramex.com} loads (post-redirect).
	 *
	 * @param entryUrl optional; defaults to profile {@code CUSTOMS_APP_URL} or {@code URL}
	 * @param email optional; defaults to profile {@code MS_LOGIN_EMAIL}, or {@code MS_IMAP_USER} when {@code MS_AUTO_FETCH_OTP_FROM_EMAIL} is true and login email is blank
	 * @param optionalPassword optional; plain password for {@link WebUI#setText} (prefer Katalon masked profile variables — never commit secrets). If blank, password step is skipped when no password page is shown.
	 * @param otpPlain optional one-time code; if blank and OTP field appears, tries IMAP when {@code MS_AUTO_FETCH_OTP_FROM_EMAIL} is true, else waits for manual entry (see {@code MS_MFA_WAIT_SECONDS}).
	 * @param skipOtp when true, skips OTP / MFA handling entirely (username + password only).
	 */
	@Keyword
	def loginToCustomsManagement(String entryUrl = null, String email = null, String optionalPassword = null, String otpPlain = null, boolean skipOtp = false) {
		try {
			ensureBrowserOpen()
			String startUrl = (entryUrl?.trim() ?: (ProfileGlobals.get('CUSTOMS_APP_URL') ?: ProfileGlobals.get('URL'))).trim()
			if (!startUrl) {
				failStop('CUSTOMS_APP_URL (or URL) must be set in the active profile.')
			}
			String user = (email ?: ProfileGlobals.get('MS_LOGIN_EMAIL')).trim()
			if (!user && 'true'.equalsIgnoreCase(ProfileGlobals.get('MS_AUTO_FETCH_OTP_FROM_EMAIL', 'false'))) {
				user = ProfileGlobals.get('MS_IMAP_USER').trim()
			}
			if (!user) {
				failStop('Set MS_LOGIN_EMAIL in the active Katalon profile (Project > Profiles > Variables), pass email to loginToCustomsManagement(...), or when MS_AUTO_FETCH_OTP_FROM_EMAIL is true set MS_IMAP_USER if it matches your Microsoft sign-in UPN.')
			}
			String passVal = (optionalPassword ?: ProfileGlobals.get('MS_LOGIN_PASSWORD')).trim()
			String otp = (otpPlain ?: ProfileGlobals.get('MS_VERIFICATION_CODE')).trim()

			WebUI.navigateToUrl(startUrl)
			WebUI.delay(3)
			WebUI.takeScreenshot()

			if (canShortCircuitWarmSessionAsAlreadySignedIn()) {
				KeywordUtil.logInfo('Already on dev-cc app — skipping Microsoft login.')
				return
			}

			if (!isOnMicrosoftLoginHost()) {
				WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
				WebUI.delay(2)
			}

			if (canShortCircuitWarmSessionAsAlreadySignedIn()) {
				return
			}

			clickSignInWithMicrosoftOnAramexLoginIfShown()

			if (canShortCircuitWarmSessionAsAlreadySignedIn()) {
				return
			}

			if (WebUI.waitForElementVisible(to('inp_ms_email'), 45, FailureHandling.OPTIONAL)) {
				enterEmailAndNext(user)
			} else if (!isOnDevCcApp() && !isOnAramexHostAfterSsoForWait()) {
				failStop('Expected Microsoft email page or dev-cc / dev.cc Aramex app URL, but got: ' + currentUrlSafe())
			}

			enterPasswordAndSignInIfShown(passVal)

			if (!skipOtp) {
				enterOtpAndVerifyIfShown(otp)
			}

			dismissStaySignedInIfConfigured()

			int totalWait = 180
			try {
				String tw = ProfileGlobals.get('MS_LOGIN_MAX_WAIT_SECONDS', '180')
				totalWait = Integer.parseInt(tw)
			} catch (NumberFormatException ignored) {
			}
			if (!waitForDevCcApp(totalWait)) {
				failStop('Timed out waiting for redirect to dev-cc / dev.cc Aramex app after Microsoft login. Current URL: ' + currentUrlSafe())
			}
			KeywordUtil.logInfo('Microsoft login flow finished — continuing on Aramex app (post-SSO).')
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("loginToCustomsManagement failed: ${e.message}")
		}
	}

	/**
	 * Same as {@link #loginToCustomsManagement()} but reads all values from the active Katalon profile.
	 */
	@Keyword
	def loginToCustomsManagementUsingProfile() {
		loginToCustomsManagement(null, null, null, null)
	}

	/**
	 * Microsoft sign-in using profile {@code Automation_User} and {@code Automation_Password} only (no OTP / MFA step).
	 */
	@Keyword
	def loginToCustomsManagementUsingAutomationProfile() {
		String user = ProfileGlobals.get('Automation_User').trim()
		String pass = ProfileGlobals.get('Automation_Password').trim()
		if (!user) {
			failStop('Set Automation_User in the active Katalon profile (Profiles/default.glbl).')
		}
		if (!pass) {
			failStop('Set Automation_Password in the active Katalon profile (Profiles/default.glbl).')
		}
		loginToCustomsManagement(null, user, pass, null, true)
	}
}
