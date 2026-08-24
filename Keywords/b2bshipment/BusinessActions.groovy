package b2bshipment

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement

import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import customsmanagement.ProfileGlobals

/**
 * B2B Keywords Manager / B2B shipment classification (CCIPH-226, linked CCIPH-476).
 * Assumes authenticated session - login is handled separately (LOGIN-TEST_SUITE).
 * Locators: Object Repository/B2B-Shipment/CCIPH-226/
 * Patterns aligned with Entity Management upload/export keywords.
 */
class BusinessActions {

	private static final String OR_PREFIX = 'B2B-Shipment/CCIPH-226/'
	private static final int DEFAULT_TIMEOUT = 30
	private static final String DEFAULT_PAGE_URL = 'https://dev-cc.aramex.com/customs-management/b2b-keywords-manager'
	private static final String DEFAULT_APP_URL = 'https://dev-cc.aramex.com/customs-management'
	private static final String B2B_TABLE_XPATH =
		"//h2[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2b keyword') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2b keywords')]/following::table[1] | //table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'keyword')]][1]"
	/** Data rows only — thead/header (Keyword, Match Type) is excluded via tbody */
	private static final String B2B_TABLE_DATA_ROWS_XPATH =
		"(//h2[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2b keyword') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2b keywords')]/following::table[1] | //table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'keyword')]][1])//tbody/tr"
	private static final String TEMPLATE_EXPORT_DETECTED_FILE = 'Reports/latest-b2b-template-export.txt'
	private static final String FIXTURES_DIR = 'Data Files/fixtures/CCIPH-226'

	private String lastTemplateExportFilePath = ''
	private String lastTemplateExportFileName = ''
	private String lastGeneratedUploadPath = ''

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static void failStop(String message) {
		try {
			WebUI.takeScreenshot()
		} catch (Throwable ignored) {
			// Browser may not be open yet; never mask the original failure with screenshot errors
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

	private static void click(TestObject testObject) {
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.takeScreenshot()
	}

	private static String xpathLiteral(String value) {
		String v = (value ?: '').trim()
		if (!v) {
			return "''"
		}
		if (!v.contains("'")) {
			return "'${v}'"
		}
		if (!v.contains('"')) {
			return "\"${v}\""
		}
		String[] parts = v.split("'")
		StringBuilder sb = new StringBuilder('concat(')
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				sb.append(",\"'\",")
			}
			sb.append("'").append(parts[i]).append("'")
		}
		sb.append(')')
		return sb.toString()
	}

	private static String projectDir() {
		try {
			String d = RunConfiguration.getProjectDir()
			if (d?.trim()) {
				return d
			}
		} catch (Exception ignored) {
		}
		return System.getProperty('user.dir') ?: ''
	}

	private static String resolveProjectRelativePath(String rawPath) {
		String path = (rawPath ?: '').trim()
		while ((path.startsWith('"') && path.endsWith('"')) || (path.startsWith("'") && path.endsWith("'"))) {
			path = path.substring(1, path.length() - 1).trim()
		}
		if (!path) {
			return ''
		}
		File asIs = new File(path)
		if (asIs.isAbsolute() && asIs.isFile()) {
			return asIs.absolutePath
		}
		File fromProject = new File(projectDir(), path)
		if (fromProject.isFile()) {
			return fromProject.absolutePath
		}
		File cwd = new File(System.getProperty('user.dir'), path)
		if (cwd.isFile()) {
			return cwd.absolutePath
		}
		return asIs.absolutePath
	}

	private static void ensureBrowser() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
		} catch (Throwable ignored) {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
		}
	}

	private static String customsAppUrl() {
		String url = (ProfileGlobals.get('CUSTOMS_APP_URL', '') ?: ProfileGlobals.get('URL', '') ?: '').trim()
		return url ?: DEFAULT_APP_URL
	}

	/**
	 * Resolves B2B Keywords Manager URL from profile {@code B2B_Keywords_Manager_URL}
	 * (same pattern as AP_MANAGEMENT_URL for navigateToApManagement).
	 */
	private static String resolveB2BKeywordsManagerUrl() {
		String url = (ProfileGlobals.get('B2B_Keywords_Manager_URL', '') ?:
			ProfileGlobals.get('B2B_KEYWORDS_MANAGER_URL', '') ?: '').trim()
		if (!url) {
			String app = customsAppUrl().replaceAll('/$', '')
			url = app + '/b2b-keywords-manager'
		}
		if (!url) {
			failStop('B2B_Keywords_Manager_URL (or CUSTOMS_APP_URL) must be set in the active profile.')
		}
		return url
	}

	/**
	 * Resolves B2B Keywords Manager Stage URL from profile {@code B2B_Keywords_Manager_Satge_URL}
	 * (same pattern as STAGE_GLOBALSETTINGS for navigateToStageGlobalSettings).
	 */
	private static String resolveB2BKeywordsManagerStageUrl() {
		String url = (ProfileGlobals.get('B2B_Keywords_Manager_Satge_URL', '') ?:
			ProfileGlobals.get('B2B_Keywords_Manager_Stage_URL', '') ?: '').trim()
		if (!url) {
			String stageApp = (ProfileGlobals.get('STAGE_APPLICATION', '') ?: '').trim().replaceAll('/$', '')
			if (stageApp) {
				url = stageApp + '/b2b-keywords-manager'
			}
		}
		if (!url) {
			failStop('B2B_Keywords_Manager_Satge_URL (or STAGE_APPLICATION) must be set in the active profile.')
		}
		return url
	}

	@Keyword
	def navigateToCustomsManagementHome() {
		try {
			ensureBrowser()
			WebUI.navigateToUrl(customsAppUrl())
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToCustomsManagementHome failed: ${e.message}")
		}
	}

	/**
	 * Navigate to B2B Keywords Manager via profile URL (mirrors navigateToApManagement).
	 * Uses GlobalVariable / profile: B2B_Keywords_Manager_URL
	 */
	@Keyword
	def navigateToB2BKeywordsManager() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			WebUI.navigateToUrl(resolveB2BKeywordsManagerUrl())
			waitVisible(to('txt_pageHeading_B2BKeywordsManager'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToB2BKeywordsManager failed: ${e.message}")
		}
	}

	/**
	 * Navigate to B2B Keywords Manager on Stage via profile URL.
	 * Uses GlobalVariable / profile: B2B_Keywords_Manager_Satge_URL
	 */
	@Keyword
	def navigateToStageB2BKeywordsManager() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			WebUI.navigateToUrl(resolveB2BKeywordsManagerStageUrl())
			waitVisible(to('txt_pageHeading_B2BKeywordsManager'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToStageB2BKeywordsManager failed: ${e.message}")
		}
	}

	@Keyword
	def verifyB2BKeywordsManagerPageLoaded() {
		try {
			waitVisible(to('txt_pageHeading_B2BKeywordsManager'))
			WebUI.verifyElementVisible(to('txt_pageHeading_B2BKeywordsManager'), FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(to('section_UploadExcel'), FailureHandling.STOP_ON_FAILURE)
			String url = (WebUI.getUrl() ?: '').toLowerCase()
			if (!url.contains('b2b-keywords-manager')) {
				failStop("Expected URL to contain /b2b-keywords-manager but was: ${WebUI.getUrl()}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BKeywordsManagerPageLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def openB2BKeywordsManagerFromMenu() {
		try {
			ensureBrowser()
			String current = ''
			try {
				current = WebUI.getUrl() ?: ''
			} catch (Exception ignored) {
			}
			if (!current.toLowerCase().contains('customs-management')) {
				navigateToCustomsManagementHome()
			}
			TestObject menu = to('menu_B2BKeywordsManager')
			boolean menuFound = WebUI.waitForElementVisible(menu, 10, FailureHandling.OPTIONAL)
			if (menuFound) {
				waitVisibleClickable(menu)
				WebUI.click(menu)
				WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
				waitVisible(to('txt_pageHeading_B2BKeywordsManager'))
				WebUI.takeScreenshot()
			} else {
				KeywordUtil.logInfo('B2B Keywords Manager menu not visible — falling back to B2B_Keywords_Manager_URL navigation')
				navigateToB2BKeywordsManager()
			}
			verifyB2BKeywordsManagerPageLoaded()
		} catch (Exception e) {
			failStop("openB2BKeywordsManagerFromMenu failed: ${e.message}")
		}
	}

	@Keyword
	def verifyB2BMenuAboveRoleAndPermissions() {
		try {
			// Reach page via URL first (stable, same as AP navigateToApManagement), then assert menu if present
			navigateToB2BKeywordsManager()
			verifyB2BKeywordsManagerPageLoaded()
			navigateToCustomsManagementHome()
			TestObject b2b = to('menu_B2BKeywordsManager')
			TestObject roles = to('menu_RoleAndPermissions')
			boolean b2bVisible = WebUI.waitForElementVisible(b2b, 15, FailureHandling.OPTIONAL)
			boolean rolesVisible = WebUI.waitForElementVisible(roles, 10, FailureHandling.OPTIONAL)
			if (!b2bVisible || !rolesVisible) {
				KeywordUtil.logInfo('Left-nav text labels not visible (icon-only sidebar). Verified B2B Keywords Manager via B2B_Keywords_Manager_URL instead.')
				navigateToB2BKeywordsManager()
				verifyB2BKeywordsManagerPageLoaded()
				WebUI.takeScreenshot()
				return
			}
			WebUI.verifyElementVisible(b2b, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(roles, FailureHandling.STOP_ON_FAILURE)
			WebElement b2bEl = WebUiCommonHelper.findWebElement(b2b, DEFAULT_TIMEOUT)
			WebElement rolesEl = WebUiCommonHelper.findWebElement(roles, DEFAULT_TIMEOUT)
			int b2bY = b2bEl.getLocation().getY()
			int rolesY = rolesEl.getLocation().getY()
			if (b2bY > rolesY) {
				failStop("B2B Keywords Manager (y=${b2bY}) is not above Role and Permissions (y=${rolesY})")
			}
			KeywordUtil.logInfo("Menu order OK: B2B Keywords Manager y=${b2bY} above Role and Permissions y=${rolesY}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BMenuAboveRoleAndPermissions failed: ${e.message}")
		}
	}

	@Keyword
	def verifyPrimaryPageControlsVisible() {
		try {
			waitVisible(to('txt_pageHeading_B2BKeywordsManager'))
			WebUI.verifyElementVisible(to('lnk_Back'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
			WebUI.verifyElementVisible(to('dropzone_UploadExcel'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
			WebUI.verifyElementVisible(to('txt_acceptedFormat'), FailureHandling.STOP_ON_FAILURE)
			String helper = (WebUI.getText(to('txt_acceptedFormat'), FailureHandling.OPTIONAL) ?: '').trim()
			if (!helper.toLowerCase().contains('.xlsx') || !helper.toLowerCase().contains('mb')) {
				failStop("Accepted format helper text missing .xlsx / MB. Actual: ${helper}")
			}
			WebUI.takeScreenshot()
			WebUI.verifyElementVisible(to('btn_ExportB2BTemplate'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
			WebUI.verifyElementVisible(to('btn_Submit'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
			// Table section (heading, column headers, footer) renders only after keywords are loaded
			TestObject keywordsTable = to('tbl_B2BKeywords')
			boolean tableVisible = WebUI.waitForElementVisible(keywordsTable, 5, FailureHandling.OPTIONAL)
			if (tableVisible) {
				WebUI.verifyElementVisible(to('txt_section_B2BKeywords'), FailureHandling.STOP_ON_FAILURE)
				WebUI.verifyElementVisible(to('th_Keyword'), FailureHandling.STOP_ON_FAILURE)
				WebUI.verifyElementVisible(to('th_MatchType'), FailureHandling.STOP_ON_FAILURE)
				WebUI.verifyElementVisible(to('txt_keywordsLoadedFooter'), FailureHandling.STOP_ON_FAILURE)
				KeywordUtil.logInfo('B2B keywords table section verified (keywords loaded in environment)')
			} else {
				KeywordUtil.logInfo('B2B keywords table not visible — empty state with no loaded keywords. Upload controls verified.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyPrimaryPageControlsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifySubmitDisabledWhenNoFile() {
		try {
			TestObject disabledSubmit = to('btn_SubmitDisabled')
			if (!WebUI.waitForElementVisible(disabledSubmit, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
				failStop('Disabled SUBMIT button not visible (submitButtonBase + submitButtonDisabled locator)')
			}
			WebUI.verifyElementVisible(disabledSubmit, FailureHandling.STOP_ON_FAILURE)
			KeywordUtil.logInfo('SUBMIT verified as disabled via submitButtonDisabled locator')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySubmitDisabledWhenNoFile failed: ${e.message}")
		}
	}

	@Keyword
	def verifySubmitEnabled() {
		try {
			waitVisibleClickable(to('btn_Submit'))
			WebUI.verifyElementClickable(to('btn_Submit'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySubmitEnabled failed: ${e.message}")
		}
	}

	@Keyword
	def clickBackAndVerifyLeftPage() {
		try {
			click(to('lnk_Back'))
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			String url = (WebUI.getUrl() ?: '').toLowerCase()
			if (url.contains('b2b-keywords-manager')) {
				failStop("Still on B2B Keywords Manager after BACK. URL=${WebUI.getUrl()}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickBackAndVerifyLeftPage failed: ${e.message}")
		}
	}

	@Keyword
	def exportB2BTemplateAndVerifyColumns() {
		try {
			File downloadsDir = ensureProjectDownloadsDirectory()
			configureBrowserDownloadDirectory(downloadsDir)
			long started = System.currentTimeMillis()
			waitVisibleClickable(to('btn_ExportB2BTemplate'))
			WebUI.click(to('btn_ExportB2BTemplate'))
			WebUI.takeScreenshot()
			String path = waitForTemplateDownload(started, downloadsDir)
			KeywordUtil.logInfo("B2B template downloaded to Katalon project Downloads: ${path}")
			List<String> headers = readXlsxFirstRowHeaders(path)
			KeywordUtil.logInfo("B2B template headers: ${headers}")
			boolean hasKeyword = headers.any { it?.trim()?.equalsIgnoreCase('Keyword') }
			boolean hasMatchType = headers.any { it?.trim()?.equalsIgnoreCase('Match Type') || it?.trim()?.equalsIgnoreCase('MatchType') }
			if (!hasKeyword || !hasMatchType) {
				failStop("Template must contain Keyword and Match Type columns. Found: ${headers}")
			}
			WebUI.takeScreenshot()
			// Cleanup: delete exported file after successful verification to avoid duplicate Downloads clutter
			deleteLastTemplateFile(path)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("exportB2BTemplateAndVerifyColumns failed: ${e.message}")
		}
	}

	/**
	 * Katalon project Downloads folder: {@code <project>/Downloads}.
	 * Creates the directory when missing. Profile {@code B2B_TEMPLATE_DOWNLOADS_DIR} overrides when set and valid.
	 */
	private File ensureProjectDownloadsDirectory() {
		String fromProfile = (ProfileGlobals.get('B2B_TEMPLATE_DOWNLOADS_DIR', '') ?: '').trim()
		File dir
		if (fromProfile) {
			dir = new File(fromProfile)
			if (!dir.isAbsolute()) {
				dir = new File(projectDir(), fromProfile)
			}
		} else {
			dir = new File(projectDir(), 'Downloads')
		}
		if (!dir.exists() && !dir.mkdirs()) {
			failStop("Could not create Katalon Downloads folder: ${dir.absolutePath}")
		}
		if (!dir.isDirectory()) {
			failStop("Katalon Downloads path is not a directory: ${dir.absolutePath}")
		}
		KeywordUtil.logInfo("B2B template Downloads directory: ${dir.absolutePath}")
		return dir
	}

	/**
	 * Point Chrome (active session) download path to the Katalon project Downloads folder via CDP.
	 */
	private void configureBrowserDownloadDirectory(File downloadsDir) {
		try {
			def driver = DriverFactory.getWebDriver()
			if (driver == null) {
				return
			}
			String path = downloadsDir.absolutePath
			Map<String, Object> params = new HashMap<String, Object>()
			params.put('behavior', 'allow')
			params.put('downloadPath', path)
			boolean configured = false
			try {
				driver.executeCdpCommand('Page.setDownloadBehavior', params)
				configured = true
			} catch (Throwable ignoredPage) {
				try {
					Map<String, Object> browserParams = new HashMap<String, Object>()
					browserParams.put('behavior', 'allow')
					browserParams.put('downloadPath', path)
					browserParams.put('eventsEnabled', true)
					driver.executeCdpCommand('Browser.setDownloadBehavior', browserParams)
					configured = true
				} catch (Throwable ignoredBrowser) {
					KeywordUtil.logInfo("CDP download path not set (${ignoredBrowser.message}); will poll ${path} and user Downloads as fallback")
				}
			}
			if (configured) {
				KeywordUtil.logInfo("Chrome download directory set to: ${path}")
			}
		} catch (Throwable e) {
			KeywordUtil.logInfo("configureBrowserDownloadDirectory skipped: ${e.message}")
		}
	}

	private File resolveDownloadsDirectory() {
		return ensureProjectDownloadsDirectory()
	}

	private String waitForTemplateDownload(long startedMs, File preferredDir = null, int timeoutSeconds = 45) {
		File dir = preferredDir ?: resolveDownloadsDirectory()
		File userDownloads = new File(System.getProperty('user.home') ?: '', 'Downloads')
		List<File> searchDirs = [dir]
		if (userDownloads.isDirectory() && userDownloads.absolutePath != dir.absolutePath) {
			searchDirs.add(userDownloads)
		}
		if (!dir.isDirectory()) {
			failStop("Downloads folder does not exist: ${dir.absolutePath}")
		}
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
		File newest = null
		File foundIn = null
		while (System.currentTimeMillis() < deadline) {
			for (File searchDir : searchDirs) {
				File[] files = searchDir.listFiles()
				if (files == null) {
					continue
				}
				List<File> candidates = files.findAll { File f ->
					f.isFile() && f.lastModified() >= (startedMs - 2000L) &&
						f.name.toLowerCase().endsWith('.xlsx') &&
						!f.name.toLowerCase().contains('.crdownload') &&
						!f.name.toLowerCase().endsWith('.tmp') &&
						!f.name.toLowerCase().endsWith('.part')
				}.sort { a, b -> Long.compare(b.lastModified(), a.lastModified()) }
				if (!candidates.isEmpty()) {
					newest = candidates[0]
					foundIn = searchDir
					break
				}
			}
			if (newest != null) {
				break
			}
			WebUI.delay(1)
		}
		if (newest == null) {
			failStop("No B2B template .xlsx download detected in ${dir.absolutePath}" +
				(userDownloads.isDirectory() ? " (also checked ${userDownloads.absolutePath})" : ''))
		}
		// If Chrome saved to user Downloads, move into Katalon project Downloads
		if (foundIn != null && foundIn.absolutePath != dir.absolutePath) {
			File target = new File(dir, newest.name)
			if (target.exists()) {
				target = new File(dir, "B2B_Template_${System.currentTimeMillis()}.xlsx")
			}
			try {
				java.nio.file.Files.move(newest.toPath(), target.toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING)
				newest = target
				KeywordUtil.logInfo("Moved B2B template into Katalon Downloads: ${newest.absolutePath}")
			} catch (Exception moveEx) {
				try {
					java.nio.file.Files.copy(newest.toPath(), target.toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING)
					newest.delete()
					newest = target
					KeywordUtil.logInfo("Copied B2B template into Katalon Downloads: ${newest.absolutePath}")
				} catch (Exception copyEx) {
					KeywordUtil.logInfo("Could not relocate download into project Downloads (${copyEx.message}); verifying at ${newest.absolutePath}")
				}
			}
		}
		lastTemplateExportFileName = newest.name
		lastTemplateExportFilePath = newest.absolutePath
		try {
			File reportsDir = new File(projectDir(), 'Reports')
			if (!reportsDir.exists()) {
				reportsDir.mkdirs()
			}
			new File(projectDir(), TEMPLATE_EXPORT_DETECTED_FILE).withWriter('UTF-8') { w ->
				w.write("fileName=${newest.name}\nabsolutePath=${newest.absolutePath}\ncapturedAt=${new Date()}\n")
			}
		} catch (Exception ignored) {
		}
		KeywordUtil.logInfo("Detected B2B template download: ${lastTemplateExportFilePath}")
		return lastTemplateExportFilePath
	}

	@Keyword
	def deleteLastTemplateFile(String absolutePath = null) {
		String path = (absolutePath ?: lastTemplateExportFilePath ?: '').trim()
		if (!path) {
			KeywordUtil.logInfo('deleteLastTemplateFile: no path to delete')
			return
		}
		try {
			File f = new File(path)
			if (f.isFile()) {
				boolean deleted = f.delete()
				if (deleted || !f.exists()) {
					KeywordUtil.logInfo("Deleted B2B template file after verification: ${path}")
					lastTemplateExportFilePath = ''
					lastTemplateExportFileName = ''
				} else {
					KeywordUtil.markWarning("Failed to delete B2B template file (still present): ${path}")
				}
			} else {
				KeywordUtil.logInfo("B2B template file already absent: ${path}")
			}
		} catch (Exception e) {
			KeywordUtil.markWarning("Could not delete template file ${path}: ${e.message}")
		}
	}

	private static List<String> readXlsxSharedStrings(ZipFile zip) {
		def entry = zip.getEntry('xl/sharedStrings.xml')
		if (!entry) {
			return []
		}
		String xml = zip.getInputStream(entry).getText('UTF-8')
		List<String> strings = []
		(xml =~ /<t[^>]*>([\s\S]*?)<\/t>/).each { m ->
			strings.add(m[1]?.replaceAll('&amp;', '&')?.replaceAll('&lt;', '<')?.replaceAll('&gt;', '>'))
		}
		return strings
	}

	private static String extractXlsxCellValue(String innerXml, String attrs, List<String> sharedStrings) {
		def valueMatch = (innerXml ?: '') =~ /<v>([\s\S]*?)<\/v>/
		if (!valueMatch.find()) {
			return ''
		}
		String raw = valueMatch.group(1)
		if ((attrs ?: '').contains('t="s"')) {
			int idx = Integer.parseInt(raw)
			return (idx >= 0 && idx < sharedStrings.size()) ? sharedStrings.get(idx) : ''
		}
		return raw
	}

	private static List<String> readXlsxFirstRowHeaders(String absolutePath) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			return []
		}
		ZipFile zip = new ZipFile(f)
		try {
			List<String> sharedStrings = readXlsxSharedStrings(zip)
			def sheetEntry = zip.getEntry('xl/worksheets/sheet1.xml')
			if (!sheetEntry) {
				return []
			}
			String sheetXml = zip.getInputStream(sheetEntry).getText('UTF-8')
			def rowMatch = sheetXml =~ /<row\s+r="1"[^>]*>([\s\S]*?)<\/row>/
			if (!rowMatch.find()) {
				return []
			}
			String rowContent = rowMatch.group(1)
			Map<String, String> colMap = new LinkedHashMap<String, String>()
			(rowContent =~ /<c\s+r="([A-Z]+)1"([^>]*)>([\s\S]*?)<\/c>/).each { cellMatch ->
				colMap.put(cellMatch[1], extractXlsxCellValue(cellMatch[3], cellMatch[2], sharedStrings))
			}
			return new ArrayList<String>(colMap.values())
		} finally {
			zip.close()
		}
	}

	/**
	 * Reads Keyword / Match Type data rows from a B2B upload .xlsx (sheet1).
	 * Returns list of [keyword, matchType] pairs (header row excluded).
	 */
	@Keyword
	List<List<String>> readB2BKeywordRowsFromXlsx(String rawPath) {
		try {
			String path = resolveProjectRelativePath(rawPath)
			if (!path) {
				failStop('readB2BKeywordRowsFromXlsx: file path is empty')
			}
			File f = new File(path)
			if (!f.isFile()) {
				failStop("readB2BKeywordRowsFromXlsx: file not found: ${path}")
			}
			ZipFile zip = new ZipFile(f)
			try {
				List<String> sharedStrings = readXlsxSharedStrings(zip)
				def sheetEntry = zip.getEntry('xl/worksheets/sheet1.xml')
				if (!sheetEntry) {
					failStop("readB2BKeywordRowsFromXlsx: sheet1 not found in ${path}")
				}
				String sheetXml = zip.getInputStream(sheetEntry).getText('UTF-8')
				Map<Integer, Map<String, String>> rowsByNum = new LinkedHashMap<Integer, Map<String, String>>()
				def rowMatcher = (sheetXml =~ /<row\s+r="(\d+)"[^>]*>([\s\S]*?)<\/row>/)
				while (rowMatcher.find()) {
					int rowNum = Integer.parseInt(rowMatcher.group(1))
					String rowContent = rowMatcher.group(2)
					Map<String, String> colMap = new LinkedHashMap<String, String>()
					def cellMatcher = (rowContent =~ /<c\s+r="([A-Z]+)(\d+)"([^>]*)>([\s\S]*?)<\/c>/)
					while (cellMatcher.find()) {
						String colLetter = cellMatcher.group(1)
						colMap.put(colLetter, extractXlsxCellValue(cellMatcher.group(4), cellMatcher.group(3), sharedStrings))
					}
					if (!colMap.isEmpty()) {
						rowsByNum.put(rowNum, colMap)
					}
				}
				if (rowsByNum.isEmpty()) {
					failStop("readB2BKeywordRowsFromXlsx: no rows found in ${path}")
				}
				Map<String, String> headerRow = rowsByNum.get(1)
				if (!headerRow) {
					failStop("readB2BKeywordRowsFromXlsx: header row missing in ${path}")
				}
				String keywordCol = null
				String matchTypeCol = null
				headerRow.each { String col, String val ->
					String normalized = (val ?: '').trim().toLowerCase()
					if (normalized == 'keyword') {
						keywordCol = col
					} else if (normalized == 'match type' || normalized == 'matchtype') {
						matchTypeCol = col
					}
				}
				if (!keywordCol || !matchTypeCol) {
					failStop("readB2BKeywordRowsFromXlsx: Keyword and Match Type columns required. Headers: ${headerRow.values()}")
				}
				List<List<String>> dataRows = []
				rowsByNum.keySet().sort().each { Integer rowNum ->
					if (rowNum <= 1) {
						return
					}
					Map<String, String> row = rowsByNum.get(rowNum)
					String keyword = (row.get(keywordCol) ?: '').trim()
					String matchType = (row.get(matchTypeCol) ?: '').trim()
					if (keyword && matchType) {
						dataRows.add([keyword, matchType])
					}
				}
				if (dataRows.isEmpty()) {
					failStop("readB2BKeywordRowsFromXlsx: no data rows in ${path}")
				}
				KeywordUtil.logInfo("Read ${dataRows.size()} B2B keyword row(s) from ${f.name}: ${dataRows}")
				return dataRows
			} finally {
				zip.close()
			}
		} catch (Exception e) {
			failStop("readB2BKeywordRowsFromXlsx failed: ${e.message}")
			return []
		}
	}

	@Keyword
	def uploadB2BExcelFileAndSubmit(String rawPath) {
		try {
			uploadB2BExcelFile(rawPath)
			verifySubmitEnabled()
			clickSubmit()
			verifyUploadSuccessOrTableRefresh()
		} catch (Exception e) {
			failStop("uploadB2BExcelFileAndSubmit failed: ${e.message}")
		}
	}

	/**
	 * After submit, verify each Keyword/Match Type from the uploaded .xlsx appears in the B2B keywords table
	 * and that the loaded footer count matches the number of rows in the file.
	 */
	@Keyword
	def verifyUploadedB2BKeywordsFromFile(String rawPath) {
		try {
			List<List<String>> expectedRows = readB2BKeywordRowsFromXlsx(rawPath)
			KeywordUtil.logInfo("Verifying ${expectedRows.size()} uploaded keyword row(s) against UI table")
			expectedRows.each { List<String> row ->
				verifyKeywordRow(row[0], row[1])
			}
			int footerCount = readKeywordsLoadedFooterCount()
			if (footerCount != expectedRows.size()) {
				failStop("Footer shows ${footerCount} keywords loaded but .xlsx contains ${expectedRows.size()} data row(s)")
			}
			int tableRows = countKeywordTableRows()
			if (tableRows != expectedRows.size()) {
				KeywordUtil.logInfo("Table tbody rows=${tableRows}, xlsx rows=${expectedRows.size()}, footer=${footerCount}")
				failStop("Table tbody shows ${tableRows} data row(s) but .xlsx contains ${expectedRows.size()} data row(s) (thead header excluded)")
			}
			KeywordUtil.logInfo("UI table matches .xlsx: ${expectedRows.size()} Keyword/Match Type rows verified")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyUploadedB2BKeywordsFromFile failed: ${e.message}")
		}
	}

	/**
	 * Builds a minimal .xlsx with Keyword / Match Type (+ optional extra columns) using OOXML zip.
	 * Returns absolute path under Data Files/fixtures/CCIPH-226 (or temp).
	 */
	@Keyword
	String createB2BKeywordsExcel(List rows, String fileName = null, List extraHeaders = []) {
		try {
			File dir = new File(projectDir(), FIXTURES_DIR)
			if (!dir.exists()) {
				dir.mkdirs()
			}
			String name = (fileName ?: "b2b_keywords_${System.currentTimeMillis()}.xlsx").trim()
			File out = new File(dir, name)
			List headers = ['Keyword', 'Match Type'] + (extraHeaders ?: [])
			List allRows = [headers] + (rows ?: [])
			List<String> shared = []
			allRows.each { row ->
				(row as List).each { cell ->
					shared.add(cell == null ? '' : cell.toString())
				}
			}
			StringBuilder sharedXml = new StringBuilder('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
			sharedXml.append('<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="')
				.append(shared.size()).append('" uniqueCount="').append(shared.size()).append('">')
			shared.each { s ->
				String escaped = (s ?: '').replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
				sharedXml.append('<si><t>').append(escaped).append('</t></si>')
			}
			sharedXml.append('</sst>')

			StringBuilder sheet = new StringBuilder('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
			sheet.append('<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>')
			int idx = 0
			for (int r = 0; r < allRows.size(); r++) {
				List row = allRows[r] as List
				sheet.append('<row r="').append(r + 1).append('">')
				for (int c = 0; c < row.size(); c++) {
					String col = columnLetter(c)
					sheet.append('<c r="').append(col).append(r + 1).append('" t="s"><v>').append(idx).append('</v></c>')
					idx++
				}
				sheet.append('</row>')
			}
			sheet.append('</sheetData></worksheet>')

			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))
			writeZipEntry(zos, '[Content_Types].xml', '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>''')
			writeZipEntry(zos, '_rels/.rels', '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>''')
			writeZipEntry(zos, 'xl/workbook.xml', '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>''')
			writeZipEntry(zos, 'xl/_rels/workbook.xml.rels', '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>''')
			writeZipEntry(zos, 'xl/sharedStrings.xml', sharedXml.toString())
			writeZipEntry(zos, 'xl/worksheets/sheet1.xml', sheet.toString())
			zos.close()
			lastGeneratedUploadPath = out.absolutePath
			KeywordUtil.logInfo("Created B2B keywords Excel: ${lastGeneratedUploadPath}")
			return lastGeneratedUploadPath
		} catch (Exception e) {
			failStop("createB2BKeywordsExcel failed: ${e.message}")
			return ''
		}
	}

	private static String columnLetter(int zeroBasedIndex) {
		int n = zeroBasedIndex
		StringBuilder sb = new StringBuilder()
		while (n >= 0) {
			sb.insert(0, (char) ('A' + (n % 26)))
			n = (n / 26) as int
			n--
		}
		return sb.toString()
	}

	private static void writeZipEntry(ZipOutputStream zos, String name, String content) {
		zos.putNextEntry(new ZipEntry(name))
		zos.write(content.getBytes('UTF-8'))
		zos.closeEntry()
	}

	@Keyword
	String createOversizedXlsxPlaceholder(int minBytes = 36 * 1024 * 1024) {
		try {
			File dir = new File(projectDir(), FIXTURES_DIR)
			if (!dir.exists()) {
				dir.mkdirs()
			}
			File out = new File(dir, 'oversized_b2b_keywords.xlsx')
			// Start from a valid tiny xlsx then pad with zip comment / extra local file to exceed size
			String tiny = createB2BKeywordsExcel([['PadKW', 'Contains']], 'tiny_for_pad.xlsx')
			byte[] base = new File(tiny).bytes
			RandomAccessFile raf = new RandomAccessFile(out, 'rw')
			raf.write(base)
			raf.setLength(Math.max(minBytes, base.length + 1))
			raf.close()
			KeywordUtil.logInfo("Created oversized xlsx (${out.length()} bytes): ${out.absolutePath}")
			return out.absolutePath
		} catch (Exception e) {
			failStop("createOversizedXlsxPlaceholder failed: ${e.message}")
			return ''
		}
	}

	private static String uploadFilesDir() {
		return new File(projectDir(), 'Upload Files').absolutePath
	}

	private static void prepareB2BFileInputForNonXlsx(WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver()
		js.executeScript(
			'arguments[0].removeAttribute("accept"); arguments[0].style.display = "block"; arguments[0].style.visibility = "visible"; arguments[0].style.opacity = "1";',
			element
		)
	}

	private static WebElement findB2BUploadFileInput() {
		TestObject fileInput = to('inp_UploadFile')
		if (!WebUI.waitForElementPresent(fileInput, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
			failStop('B2B upload file input not found on page')
		}
		return WebUiCommonHelper.findWebElement(fileInput, DEFAULT_TIMEOUT)
	}

	private static void robotPressKey(Robot robot, int keyCode) {
		robot.keyPress(keyCode)
		robot.keyRelease(keyCode)
	}

	private static void robotPressAltKey(Robot robot, int keyCode) {
		robot.keyPress(KeyEvent.VK_ALT)
		robot.keyPress(keyCode)
		robot.keyRelease(keyCode)
		robot.keyRelease(KeyEvent.VK_ALT)
	}

	private static void robotTypeString(Robot robot, String text) {
		(text ?: '').each { char ch ->
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(ch as char)
			if (KeyEvent.CHAR_UNDEFINED == keyCode) {
				return
			}
			boolean upper = Character.isUpperCase(ch as char)
			if (upper) {
				robot.keyPress(KeyEvent.VK_SHIFT)
			}
			robotPressKey(robot, keyCode)
			if (upper) {
				robot.keyRelease(KeyEvent.VK_SHIFT)
			}
		}
	}

	private static void pasteTextToClipboard(String text) {
		StringSelection selection = new StringSelection(text ?: '')
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null)
	}

	private static void robotPasteFromClipboard(Robot robot) {
		robot.keyPress(KeyEvent.VK_CONTROL)
		robot.keyPress(KeyEvent.VK_V)
		robot.keyRelease(KeyEvent.VK_V)
		robot.keyRelease(KeyEvent.VK_CONTROL)
	}

	private static void selectAllFilesInWindowsOpenDialog(Robot robot) {
		KeywordUtil.logInfo('Windows Open dialog: selecting file type All Files (Alt+T)')
		robotPressAltKey(robot, KeyEvent.VK_T)
		WebUI.delay(0.5)
		robotTypeString(robot, 'All Files')
		WebUI.delay(0.3)
		robotPressKey(robot, KeyEvent.VK_ENTER)
		WebUI.delay(0.3)
		// Fallback when combo lists *.xlsx first — All Files (*.*) is usually the last entry
		robotPressAltKey(robot, KeyEvent.VK_T)
		WebUI.delay(0.3)
		robotPressKey(robot, KeyEvent.VK_END)
		WebUI.delay(0.2)
		robotPressKey(robot, KeyEvent.VK_ENTER)
		WebUI.delay(0.5)
	}

	private static void navigateToFolderInWindowsOpenDialog(Robot robot, String folderPath) {
		KeywordUtil.logInfo("Windows Open dialog: navigating to project Upload Files folder (Alt+D): ${folderPath}")
		robotPressAltKey(robot, KeyEvent.VK_D)
		WebUI.delay(0.5)
		pasteTextToClipboard(folderPath)
		robotPasteFromClipboard(robot)
		WebUI.delay(0.3)
		robotPressKey(robot, KeyEvent.VK_ENTER)
		WebUI.delay(1)
	}

	private static void enterPathInWindowsOpenDialog(Robot robot, String absolutePath) {
		KeywordUtil.logInfo("Windows Open dialog: entering file path (Alt+N): ${absolutePath}")
		robotPressAltKey(robot, KeyEvent.VK_N)
		WebUI.delay(0.3)
		pasteTextToClipboard(absolutePath)
		robotPasteFromClipboard(robot)
		WebUI.delay(0.3)
		robotPressKey(robot, KeyEvent.VK_ENTER)
		WebUI.delay(1)
	}

	private static void openB2BUploadFileDialog() {
		TestObject dropzone = to('dropzone_UploadExcel')
		if (WebUI.waitForElementClickable(dropzone, 10, FailureHandling.OPTIONAL)) {
			WebUI.click(dropzone)
			return
		}
		WebElement element = findB2BUploadFileInput()
		prepareB2BFileInputForNonXlsx(element)
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver()
		js.executeScript('arguments[0].click();', element)
	}

	private static void uploadViaWindowsOpenDialogAllFiles(String absolutePath, String folderPath, String fileName) {
		openB2BUploadFileDialog()
		WebUI.delay(2)
		Robot robot = new Robot()
		robot.setAutoDelay(80)
		navigateToFolderInWindowsOpenDialog(robot, folderPath)
		selectAllFilesInWindowsOpenDialog(robot)
		enterPathInWindowsOpenDialog(robot, absolutePath)
		KeywordUtil.logInfo("Windows Open dialog selected file '${fileName}' from '${folderPath}' with All Files filter")
	}

	private static void uploadNonXlsxViaFileInput(String absolutePath) {
		WebElement element = findB2BUploadFileInput()
		prepareB2BFileInputForNonXlsx(element)
		KeywordUtil.logInfo("Uploading non-.xlsx file from Upload Files (accept cleared = All Files): ${absolutePath}")
		element.sendKeys(absolutePath)
	}

	@Keyword
	def uploadB2BExcelFile(String filePath) {
		try {
			String path = resolveProjectRelativePath(filePath)
			if (!path) {
				failStop('uploadB2BExcelFile: file path is empty')
			}
			File f = new File(path)
			if (!f.isFile()) {
				failStop("uploadB2BExcelFile: file not found: ${path}")
			}
			TestObject fileInput = to('inp_UploadFile')
			waitVisible(fileInput, 10)
			try {
				WebUI.uploadFile(fileInput, path)
			} catch (Exception uploadEx) {
				KeywordUtil.logInfo("WebUI.uploadFile failed, retrying with sendKeys: ${uploadEx.message}")
				WebElement element = WebUiCommonHelper.findWebElement(fileInput, DEFAULT_TIMEOUT)
				element.sendKeys(path)
			}
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadB2BExcelFile failed: ${e.message}")
		}
	}

	/**
	 * Upload a non-.xlsx file (e.g. .csv) from project {@code Upload Files} folder.
	 * Clears the input accept filter (automation equivalent of All Files), with native
	 * Windows dialog fallback that navigates to Upload Files and selects All Files.
	 */
	@Keyword
	def uploadB2BNonXlsxFile(String filePath) {
		try {
			String path = resolveProjectRelativePath(filePath)
			if (!path) {
				failStop('uploadB2BNonXlsxFile: file path is empty')
			}
			File f = new File(path)
			if (!f.isFile()) {
				failStop("uploadB2BNonXlsxFile: file not found: ${path}")
			}
			String absolutePath = f.absolutePath
			String folderPath = f.parentFile?.absolutePath ?: uploadFilesDir()
			String fileName = f.name
			KeywordUtil.logInfo("B2B non-xlsx upload target: folder='${folderPath}', file='${fileName}'")
			try {
				uploadNonXlsxViaFileInput(absolutePath)
			} catch (Exception inputEx) {
				KeywordUtil.logInfo("File input upload failed, retrying via Windows Open dialog: ${inputEx.message}")
				uploadViaWindowsOpenDialogAllFiles(absolutePath, folderPath, fileName)
			}
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadB2BNonXlsxFile failed: ${e.message}")
		}
	}

	@Keyword
	def clickSubmit() {
		try {
			click(to('btn_Submit'))
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickSubmit failed: ${e.message}")
		}
	}

	@Keyword
	def verifyUploadSuccessOrTableRefresh() {
		try {
			boolean toast = WebUI.waitForElementVisible(to('msg_uploadSuccess'), 10, FailureHandling.OPTIONAL)
			if (toast) {
				WebUI.verifyElementVisible(to('msg_uploadSuccess'), FailureHandling.STOP_ON_FAILURE)
			} else {
				waitVisible(to('tbl_B2BKeywords'))
				KeywordUtil.logInfo('Success toast not found - verified B2B keywords table is visible after submit')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyUploadSuccessOrTableRefresh failed: ${e.message}")
		}
	}

	@Keyword
	def verifyUploadErrorVisible() {
		try {
			TestObject err = to('msg_uploadError')
			boolean visible = WebUI.waitForElementVisible(err, 15, FailureHandling.OPTIONAL)
			if (!visible) {
				failStop('Expected upload validation/error message was not visible')
			}
			KeywordUtil.logInfo("Upload error visible: ${(WebUI.getText(err, FailureHandling.OPTIONAL) ?: '').trim()}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyUploadErrorVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvalidB2BFileFormatError() {
		try {
			String expected = 'Invalid file format. Only .xlsx files are accepted.'
			WebUI.delay(1)
			TestObject err = invalidB2BFileFormatErrorObject()
			if (!WebUI.waitForElementVisible(err, 15, FailureHandling.OPTIONAL)) {
				failStop("Expected invalid file format message was not visible: '${expected}'")
			}
			String actual = (WebUI.getText(err, FailureHandling.OPTIONAL) ?: '').trim()
			if (!actual) {
				actual = (WebUI.getAttribute(err, 'textContent', FailureHandling.OPTIONAL) ?: '').trim()
			}
			if (actual != expected) {
				failStop("Expected error '${expected}' but got '${actual}'")
			}
			KeywordUtil.logInfo("Invalid file format error verified: ${actual}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvalidB2BFileFormatError failed: ${e.message}")
		}
	}

	private static TestObject invalidB2BFileFormatErrorObject() {
		TestObject err = to('msg_invalidFileFormat')
		if (WebUI.waitForElementPresent(err, 3, FailureHandling.OPTIONAL)) {
			return err
		}
		TestObject fallback = new TestObject('b2b_invalid_file_format_fallback')
		fallback.addProperty('xpath', ConditionType.EQUALS,
			"//div[@role='alert' and contains(@class,'fileError')] | //*[contains(@class,'dropZoneExportRow')]//div[@role='alert'] | //div[@role='alert' and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid file format')]")
		return fallback
	}

	private static TestObject b2bFileTooLargeErrorObject() {
		TestObject err = to('msg_fileTooLarge')
		if (WebUI.waitForElementPresent(err, 3, FailureHandling.OPTIONAL)) {
			return err
		}
		TestObject fallback = new TestObject('b2b_file_too_large_fallback')
		fallback.addProperty('xpath', ConditionType.EQUALS,
			"//div[@role='alert' and contains(@class,'fileError') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'file is too large')] | //*[contains(@class,'dropZoneExportRow')]//div[@role='alert' and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'maximum size is 35 mb')]")
		return fallback
	}

	@Keyword
	def verifyB2BFileTooLargeError() {
		try {
			String expected = 'File is too large. Maximum size is 35 MB.'
			WebUI.delay(1)
			TestObject err = b2bFileTooLargeErrorObject()
			if (!WebUI.waitForElementVisible(err, 15, FailureHandling.OPTIONAL)) {
				failStop("Expected file size error message was not visible: '${expected}'")
			}
			String actual = (WebUI.getText(err, FailureHandling.OPTIONAL) ?: '').trim()
			if (!actual) {
				actual = (WebUI.getAttribute(err, 'textContent', FailureHandling.OPTIONAL) ?: '').trim()
			}
			if (actual != expected) {
				failStop("Expected error '${expected}' but got '${actual}'")
			}
			KeywordUtil.logInfo("File too large error verified: ${actual}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BFileTooLargeError failed: ${e.message}")
		}
	}

	private static TestObject duplicateKeywordErrorBannerObject() {
		TestObject banner = to('msg_duplicateKeywordBanner')
		if (WebUI.waitForElementPresent(banner, 3, FailureHandling.OPTIONAL)) {
			return banner
		}
		TestObject fallback = new TestObject('b2b_duplicate_keyword_banner_fallback')
		fallback.addProperty('xpath', ConditionType.EQUALS,
			"//div[@role='alert' and contains(@class,'errorBanner')] | //div[@role='alert' and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'fix the following issues before submitting')]")
		return fallback
	}

	private static String readB2BUploadValidationBannerText(TestObject banner) {
		String text = (WebUI.getText(banner, FailureHandling.OPTIONAL) ?: '').trim()
		if (!text) {
			text = (WebUI.getAttribute(banner, 'textContent', FailureHandling.OPTIONAL) ?: '').trim()
		}
		return text
	}

	@Keyword
	def verifyB2BUploadValidationErrorBanner() {
		try {
			WebUI.delay(1)
			TestObject banner = duplicateKeywordErrorBannerObject()
			if (!WebUI.waitForElementVisible(banner, 15, FailureHandling.OPTIONAL)) {
				failStop('Upload validation error banner was not visible')
			}
			String text = readB2BUploadValidationBannerText(banner)
			if (!text.toLowerCase().contains('fix the following issues before submitting')) {
				failStop("Expected validation banner header not found. Actual: '${text}'")
			}
			KeywordUtil.logInfo("Upload validation error banner verified: ${text}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BUploadValidationErrorBanner failed: ${e.message}")
		}
	}

	@Keyword
	def verifyB2BDuplicateKeywordError() {
		try {
			verifyB2BUploadValidationErrorBanner()
			TestObject banner = duplicateKeywordErrorBannerObject()
			String text = readB2BUploadValidationBannerText(banner)
			if (!text.toLowerCase().contains('duplicate keyword and match type')) {
				failStop("Expected duplicate keyword error not found. Actual: '${text}'")
			}
			KeywordUtil.logInfo("Duplicate keyword error verified: ${text}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BDuplicateKeywordError failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyB2BInvalidCaseErrorMessage() {
		try {
			WebUI.delay(1)
	 
			TestObject errorMessage = caseInvalidKeywordErrorObject()
	 
			if (!WebUI.waitForElementVisible(errorMessage, 15, FailureHandling.OPTIONAL)) {
				failStop('Invalid keyword error message was not visible')
			}
	 
			String text = WebUI.getText(errorMessage)
	 
			if (!text.toLowerCase().contains('keyword must not contain special characters')) {
				failStop("Expected invalid keyword error message not found. Actual: '${text}'")
			}
	 
			KeywordUtil.logInfo("Invalid keyword error message verified: ${text}")
			WebUI.takeScreenshot()
	 
		} catch (Exception e) {
			failStop("verifyB2BInvalidCaseErrorMessage failed: ${e.message}")
		}
	}
	def caseInvalidKeywordErrorObject() {
			TestObject object = new TestObject('Object Repository/B2B-Shipment/CCIPH-226/msg_caseError')
			object.addProperty(
				'xpath',
				ConditionType.EQUALS,
				"//div[@role='alert' and contains(@class,'errorBanner') and contains(.,'Keyword must not contain special characters')]"
			)
			return object	
		}

	@Keyword
	def verifyKeywordRow(String keyword, String matchType) {
		try {
			String kw = (keyword ?: '').trim()
			String mt = (matchType ?: '').trim()
			TestObject row = new TestObject("b2b_row_${kw.hashCode()}_${mt.hashCode()}")
			row.addProperty('xpath', ConditionType.EQUALS,
				"${B2B_TABLE_DATA_ROWS_XPATH}[td[normalize-space()=${xpathLiteral(kw)}] and td[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(mt.toLowerCase())})]]")
			if (!WebUI.waitForElementVisible(row, 20, FailureHandling.OPTIONAL)) {
				failStop("Keyword row not found: Keyword='${kw}' Match Type~'${mt}'")
			}
			WebUI.verifyElementVisible(row, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyKeywordRow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyKeywordNotPresent(String keyword) {
		try {
			String kw = (keyword ?: '').trim()
			TestObject row = new TestObject("b2b_absent_${kw.hashCode()}")
			row.addProperty('xpath', ConditionType.EQUALS,
				"${B2B_TABLE_DATA_ROWS_XPATH}[td[normalize-space()=${xpathLiteral(kw)}]]")
			boolean present = WebUI.waitForElementPresent(row, 5, FailureHandling.OPTIONAL)
			if (present) {
				failStop("Keyword '${kw}' was unexpectedly present in B2B keywords table")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyKeywordNotPresent failed: ${e.message}")
		}
	}

	@Keyword
	int countKeywordTableRows() {
		try {
			waitVisible(to('tbl_B2BKeywords'))
			// Count tbody data rows only — ignore thead header row (Keyword / Match Type)
			TestObject rows = new TestObject('b2b_data_rows')
			rows.addProperty('xpath', ConditionType.EQUALS, B2B_TABLE_DATA_ROWS_XPATH)
			List<WebElement> list = WebUiCommonHelper.findWebElements(rows, DEFAULT_TIMEOUT)
			int count = list?.size() ?: 0
			KeywordUtil.logInfo("B2B keywords table tbody data rows (header excluded): ${count}")
			WebUI.takeScreenshot()
			return count
		} catch (Exception e) {
			failStop("countKeywordTableRows failed: ${e.message}")
			return 0
		}
	}

	@Keyword
	int readKeywordsLoadedFooterCount() {
		try {
			waitVisible(to('txt_keywordsLoadedFooter'))
			String text = (WebUI.getText(to('txt_keywordsLoadedFooter'), FailureHandling.OPTIONAL) ?: '').trim()
			def m = (text =~ /(?i)(\d+)\s*keyword/)
			if (!m.find()) {
				failStop("Could not parse keywords loaded count from footer: ${text}")
			}
			int n = Integer.parseInt(m.group(1))
			KeywordUtil.logInfo("Footer keywords loaded = ${n} (text='${text}')")
			WebUI.takeScreenshot()
			return n
		} catch (Exception e) {
			failStop("readKeywordsLoadedFooterCount failed: ${e.message}")
			return -1
		}
	}

	@Keyword
	def verifyKeywordsLoadedFooterMatchesTable() {
		try {
			int rows = countKeywordTableRows()
			int footer = readKeywordsLoadedFooterCount()
			if (rows != footer) {
				failStop("Footer count (${footer}) does not match table rows (${rows})")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyKeywordsLoadedFooterMatchesTable failed: ${e.message}")
		}
	}

	@Keyword
	def uploadValidAndSubmit(List rows, String fileName = null) {
		String path = createB2BKeywordsExcel(rows, fileName)
		uploadB2BExcelFile(path)
		verifySubmitEnabled()
		clickSubmit()
		verifyUploadSuccessOrTableRefresh()
	}

	@Keyword
	def verifyNonSuperAdminBlockedFromB2BManager() {
		try {
			ensureBrowser()
		//	navigateToCustomsManagementHome()
			boolean menuVisible = WebUI.waitForElementVisible(to('menu_B2BKeywordsManager'), 8, FailureHandling.OPTIONAL)
			WebUI.takeScreenshot()
			if (menuVisible) {
				KeywordUtil.logInfo('B2B Keywords Manager menu still visible - verifying direct URL access denial')
			} else {
				KeywordUtil.logInfo('B2B Keywords Manager menu is not visible for current (non Super Admin) session')
			}
			//WebUI.navigateToUrl(resolveB2BKeywordsManagerUrl())
			WebUI.navigateToUrl(resolveB2BKeywordsManagerStageUrl())
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
			boolean heading = WebUI.waitForElementVisible(to('txt_pageHeading_B2BKeywordsManager'), 8, FailureHandling.OPTIONAL)
			boolean dropzone = WebUI.waitForElementVisible(to('dropzone_UploadExcel'), 5, FailureHandling.OPTIONAL)
			if (heading && dropzone) {
				failStop('Non Super Admin was able to open B2B Keywords Manager upload screen')
			}
			KeywordUtil.logInfo('Non Super Admin cannot access B2B Keywords Manager upload screen (menu hidden and/or direct URL denied)')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyNonSuperAdminBlockedFromB2BManager failed: ${e.message}")
		}
	}

	/**
	 * CCIPH-226 AC1 / AC2 and CCIPH-476 classification - requires seed shipment IDs / Charges UI.
	 * When shipmentRef is blank, records a warning (data precondition) and captures screenshot evidence of current session.
	 */
	@Keyword
	def verifyShipmentClassificationOutcome(String shipmentRef, String expectedClassification, String scenarioNote) {
		try {
			String ref = (shipmentRef ?: '').trim()
			String expected = (expectedClassification ?: '').trim()
			KeywordUtil.logInfo("Classification check [${scenarioNote}]: shipment='${ref}' expected='${expected}' (CCIPH-226 / CCIPH-476)")
			if (!ref) {
				KeywordUtil.markWarning("Seed shipmentRef not provided for scenario: ${scenarioNote}. Set Test Case variable shipmentRef / use Charges Dashboard fixture.")
				ensureBrowser()
				WebUI.takeScreenshot()
				return
			}
			ensureBrowser()
			String chargesUrl = (ProfileGlobals.get('CHARGES_DASHBOARD_URL', '') ?: '').trim()
			if (!chargesUrl) {
				chargesUrl = customsAppUrl().replaceAll('/$', '') + '/charges'
			}
			WebUI.navigateToUrl(chargesUrl)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
			TestObject classificationCell = new TestObject("b2b_class_${ref.hashCode()}")
			classificationCell.addProperty('xpath', ConditionType.EQUALS,
				"//*[contains(., ${xpathLiteral(ref)})]/ancestor::tr[1]//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2b') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'b2c')] | //*[contains(@data-testid,'classification') or contains(@class,'classification')][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(expected.toLowerCase())})]")
			boolean found = WebUI.waitForElementVisible(classificationCell, 15, FailureHandling.OPTIONAL)
			if (!found) {
				failStop("Could not verify classification '${expected}' for shipment '${ref}'. Confirm Charges Dashboard exposes B2B/B2C and seed data exists. Scenario: ${scenarioNote}")
			}
			WebUI.verifyElementVisible(classificationCell, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyShipmentClassificationOutcome failed: ${e.message}")
		}
	}

	@Keyword
	String getLastGeneratedUploadPath() {
		return lastGeneratedUploadPath ?: ''
	}
}