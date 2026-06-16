package emailnotification

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.ProfileGlobals

/**
 * Custom Keywords for Global Settings email notification verification (CCIPH-39).
 * Located under Keywords/emailnotification/ — refresh the Katalon project after pull.
 */
class EmailNotificationKeywords {

	private static final String OR_PREFIX = 'Global Settings/EmailNoti/'
	private static final int DEFAULT_TIMEOUT = 30

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static void failStop(String message) {
		try {
			if (DriverFactory.getWebDriver() != null) {
				WebUI.takeScreenshot()
			}
		} catch (Exception ignored) {
		}
		KeywordUtil.markFailedAndStop(message)
	}

	private static void waitVisible(TestObject testObject, int timeout) {
		WebUI.waitForElementVisible(testObject, timeout)
	}

	private static void waitVisibleClickable(TestObject testObject, int timeout) {
		WebUI.waitForElementVisible(testObject, timeout)
		WebUI.waitForElementClickable(testObject, timeout)
	}

	private static void click(TestObject testObject) {
		waitVisibleClickable(testObject, DEFAULT_TIMEOUT)
		WebUI.click(testObject)
		WebUI.takeScreenshot()
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && v.trim()) {
				return v.trim()
			}
		}
		return ''
	}

	private static Map buildImapConfigFromProfile() {
		String user = firstNonBlank(
			ProfileGlobals.get('GS_EMAIL_IMAP_USER'),
			ProfileGlobals.get('MS_IMAP_USER'),
			ProfileGlobals.get('MS_LOGIN_EMAIL')
		)
		String host = firstNonBlank(
			ProfileGlobals.get('GS_EMAIL_IMAP_HOST'),
			ProfileGlobals.get('MS_IMAP_HOST')
		)
		if (!host && user) {
			host = 'outlook.office365.com'
		}
		String port = firstNonBlank(
			ProfileGlobals.get('GS_EMAIL_IMAP_PORT'),
			ProfileGlobals.get('MS_IMAP_PORT', '993')
		) ?: '993'
		String password = firstNonBlank(
			ProfileGlobals.get('GS_EMAIL_IMAP_PASSWORD'),
			ProfileGlobals.get('MS_IMAP_PASSWORD'),
			System.getenv('MS_IMAP_PASSWORD') ?: '',
			System.getProperty('ms.imap.password') ?: ''
		)
		return [host: host, port: port, user: user, password: password]
	}

	private static void assertImapConfigReady(Map imapConfig) {
		List missing = []
		if (!imapConfig.host) {
			missing << 'MS_IMAP_HOST (or GS_EMAIL_IMAP_HOST)'
		}
		if (!imapConfig.user) {
			missing << 'MS_IMAP_USER or MS_LOGIN_EMAIL'
		}
		if (!imapConfig.password) {
			missing << 'MS_IMAP_PASSWORD'
		}
		if (!missing.isEmpty()) {
			KeywordUtil.markFailedAndStop(
				"IMAP is not configured for email notification. Missing: ${missing.join(', ')}. " +
				"Set MS_IMAP_PASSWORD in Katalon: Project > Profiles > [active profile] > MS_IMAP_PASSWORD " +
				"(Microsoft app password). Save the profile, then re-run. " +
				"Alternatively set environment variable MS_IMAP_PASSWORD for local runs. " +
				"Mailbox user falls back to MS_LOGIN_EMAIL; host defaults to outlook.office365.com."
			)
		}
	}

	/** Call before email verification to fail fast with a clear profile setup message. */
	@Keyword
	def assertImapProfileConfiguredForEmailNotification() {
		assertImapConfigReady(buildImapConfigFromProfile())
		KeywordUtil.logInfo('IMAP profile is configured for email notification verification.')
	}

	private static String profileOrDefault(String key, String defaultValue) {
		String v = ProfileGlobals.get(key, defaultValue).trim()
		return v ?: defaultValue
	}

	private static boolean textContainsIgnoreCase(String haystack, String needle) {
		if (!needle) {
			return true
		}
		return (haystack ?: '').toLowerCase().contains(needle.toLowerCase())
	}

	private static String extractLabeledEmailValue(String normalizedBody, String label) {
		if (!normalizedBody || !label) {
			return ''
		}
		String lower = normalizedBody.toLowerCase()
		String labelLower = label.toLowerCase()
		int idx = lower.indexOf(labelLower)
		if (idx < 0) {
			return ''
		}
		String after = normalizedBody.substring(idx + label.length()).trim()
		if (after.startsWith(':')) {
			after = after.substring(1).trim()
		}
		List stopLabels = [
			'Action Performed', 'Rule Type', 'Action Summary', 'Performed By',
			'Date & Time', 'Date and Time', 'Impact', 'Rule Details'
		]
		int end = after.length()
		for (String stop : stopLabels) {
			if (stop.equalsIgnoreCase(label)) {
				continue
			}
			int stopIdx = after.toLowerCase().indexOf(stop.toLowerCase())
			if (stopIdx > 0 && stopIdx < end) {
				end = stopIdx
			}
		}
		return after.substring(0, end).trim()
	}

	private static boolean isEmailMapEmpty(Map email) {
		return email == null || email.isEmpty()
	}

	private static Map findMatchingNotificationEmail(def pollStartMillis, String subjectContains, String senderContains) {
		Map imapConfig = buildImapConfigFromProfile()
		assertImapConfigReady(imapConfig)
		int maxWait = Integer.parseInt(profileOrDefault('GS_EMAIL_FETCH_MAX_WAIT_SECONDS', '120'))
		int pollSec = Integer.parseInt(profileOrDefault('GS_EMAIL_FETCH_POLL_SECONDS', '10'))
		long deadline = System.currentTimeMillis() + (maxWait * 1000L)
		long startMs = pollStartMillis as long
		Date since = new Date(startMs - 5000L)

		while (System.currentTimeMillis() < deadline) {
			List messages = ImapMailReader.fetchMessagesSince(imapConfig, since)
			for (def msg : messages) {
				String from = (msg.from ?: '') as String
				String subject = (msg.subject ?: '') as String
				if (!textContainsIgnoreCase(from, senderContains)) {
					continue
				}
				if (!textContainsIgnoreCase(subject, subjectContains)) {
					continue
				}
				KeywordUtil.logInfo("Matched notification email. Subject: '${subject}'")
				return msg as Map
			}
			Thread.sleep(Math.max(2, pollSec) * 1000L)
		}
		return [:]
	}

	private void verifyGlobalSettingCreatedNotificationEmailInternal(
			def pollStartMillis,
			String expectedSubject,
			String expectedActionPerformed,
			String expectedRuleType,
			String expectedActionSummary,
			String expectedPerformedBy,
			String senderContains) {
		Map email = findMatchingNotificationEmail(pollStartMillis, expectedSubject, senderContains)
		if (isEmailMapEmpty(email)) {
			failStop("No notification email found. Subject contains '${expectedSubject}', sender contains '${senderContains}'.")
		}

		String emailSubject = (email.subject ?: '') as String
		String emailBody = (email.body ?: '') as String
		String normalized = ImapMailReader.normalizeEmailText(emailSubject + '\n' + emailBody)

		if (!textContainsIgnoreCase(normalized, 'Global Settings')) {
			failStop('Notification email body does not mention Global Settings.')
		}
		if (!textContainsIgnoreCase(normalized, 'CREATED')) {
			failStop('Notification email body does not mention CREATED action.')
		}

		String actionPerformed = extractLabeledEmailValue(normalized, 'Action Performed')
		if (expectedActionPerformed && !textContainsIgnoreCase(actionPerformed ?: normalized, expectedActionPerformed)) {
			failStop("Action Performed mismatch. Expected '${expectedActionPerformed}', parsed '${actionPerformed}'.")
		}

		String ruleType = extractLabeledEmailValue(normalized, 'Rule Type')
		if (expectedRuleType && !textContainsIgnoreCase(ruleType ?: normalized, expectedRuleType)) {
			failStop("Rule Type mismatch. Expected '${expectedRuleType}', parsed '${ruleType}'.")
		}

		if (expectedActionSummary && !textContainsIgnoreCase(normalized, expectedActionSummary)) {
			failStop("Action Summary mismatch. Expected text containing '${expectedActionSummary}'.")
		}

		if (expectedPerformedBy) {
			String performedBy = extractLabeledEmailValue(normalized, 'Performed By')
			if (!textContainsIgnoreCase(performedBy ?: normalized, expectedPerformedBy)) {
				failStop("Performed By mismatch. Expected '${expectedPerformedBy}', parsed '${performedBy}'.")
			}
		}

		KeywordUtil.logInfo('Global Setting Created notification email verified successfully.')
		WebUI.takeScreenshot()
	}

	/** Returns current time in millis — call immediately before the action that should trigger email. */
	@Keyword
	def recordPollStartMillis() {
		return System.currentTimeMillis()
	}

	/** Verifies on-screen rule created success message (modal or toast). */
	@Keyword
	def verifyRuleCreatedSuccessfullyOnScreen(String expectedTitle) {
		try {
			String titleText = (expectedTitle ?: 'Rule Created Successfully').trim()
			TestObject title = to('msg_ruleCreatedSuccessTitle')
			waitVisible(title, DEFAULT_TIMEOUT)
			String actual = (WebUI.getText(title, FailureHandling.OPTIONAL) ?: '').trim()
			if (!textContainsIgnoreCase(actual, titleText)) {
				failStop("Expected rule created success title containing '${titleText}', got '${actual}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleCreatedSuccessfullyOnScreen failed: ${e.message}")
		}
	}

	/** Clicks OK on rule-created success modal when present. */
	@Keyword
	def dismissRuleCreatedSuccessModalIfPresent() {
		try {
			if (WebUI.waitForElementPresent(to('btn_ruleCreatedSuccessOk'), 6, FailureHandling.OPTIONAL)) {
				click(to('btn_ruleCreatedSuccessOk'))
			}
		} catch (Exception e) {
			failStop("dismissRuleCreatedSuccessModalIfPresent failed: ${e.message}")
		}
	}

	/**
	 * Polls IMAP and verifies Global Setting Created notification email content.
	 */
	@Keyword
	def verifyGlobalSettingCreatedNotificationEmail(
			def pollStartMillis,
			String expectedSubject,
			String expectedActionPerformed,
			String expectedRuleType,
			String expectedActionSummary,
			String expectedPerformedBy) {
		try {
			String subject = (expectedSubject ?: profileOrDefault('GS_EMAIL_SUBJECT_CREATE', 'Global Setting Created')).trim()
			String sender = profileOrDefault('GS_EMAIL_SENDER_CONTAINS', 'noreply').trim()
			verifyGlobalSettingCreatedNotificationEmailInternal(
				pollStartMillis,
				subject,
				(expectedActionPerformed ?: 'Create Rule').trim(),
				(expectedRuleType ?: 'DDU').trim(),
				(expectedActionSummary ?: '').trim(),
				(expectedPerformedBy ?: ProfileGlobals.get('MS_LOGIN_EMAIL')).trim(),
				sender
			)
		} catch (Exception e) {
			failStop("verifyGlobalSettingCreatedNotificationEmail failed: ${e.message}")
		}
	}

	/** Map-based overload for advanced callers. */
	@Keyword
	def verifyGlobalSettingCreatedNotificationEmailFromMap(def args) {
		try {
			Map params = (args ?: [:]) as Map
			def pollStartMillis = params.pollStartMillis != null ? params.pollStartMillis : recordPollStartMillis()
			verifyGlobalSettingCreatedNotificationEmail(
				pollStartMillis,
				(params.expectedSubject ?: profileOrDefault('GS_EMAIL_SUBJECT_CREATE', 'Global Setting Created')) as String,
				(params.expectedActionPerformed ?: 'Create Rule') as String,
				(params.expectedRuleType ?: 'DDU') as String,
				(params.expectedActionSummary ?: '') as String,
				(params.expectedPerformedBy ?: ProfileGlobals.get('MS_LOGIN_EMAIL')) as String
			)
		} catch (Exception e) {
			failStop("verifyGlobalSettingCreatedNotificationEmailFromMap failed: ${e.message}")
		}
	}

	/** Waits and asserts no matching notification email arrived (CANCEL flows). */
	@Keyword
	def verifyNoGlobalSettingNotificationEmailSince(def pollStartMillis, String expectedSubject, def waitSeconds) {
		try {
			String subject = (expectedSubject ?: profileOrDefault('GS_EMAIL_SUBJECT_CREATE', 'Global Setting Created')).trim()
			String sender = profileOrDefault('GS_EMAIL_SENDER_CONTAINS', 'noreply').trim()
			int waitSec = (waitSeconds as int) > 0 ? (waitSeconds as int) : Integer.parseInt(profileOrDefault('GS_EMAIL_NEGATIVE_WAIT_SECONDS', '60'))

			Thread.sleep(waitSec * 1000L)
			Map email = findMatchingNotificationEmail(pollStartMillis, subject, sender)
			if (!isEmailMapEmpty(email)) {
				failStop("Unexpected notification email received. Subject: '${email.subject}'")
			}
			KeywordUtil.logInfo('No Global Settings notification email received (as expected).')
		} catch (Exception e) {
			failStop("verifyNoGlobalSettingNotificationEmailSince failed: ${e.message}")
		}
	}
}
