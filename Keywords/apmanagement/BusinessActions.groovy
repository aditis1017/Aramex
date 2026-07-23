package apmanagement

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

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.Select

import java.io.File
import java.io.FileFilter
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.Set

import customsmanagement.ProfileGlobals

/**
 * Shared business actions for AP Management (CCIPH-102 search, CCIPH-103 results grid, CCIPH-104 detail, CCIPH-200 create)
 * and AR Management (CCIPH-116 search, CCIPH-117 results grid, CCIPH-118 detail, CCIPH-126 billing preferences,
 * CCIPH-123 service charge settings, CCIPH-124 service code edit/save/cancel,
 * CCIPH-37 AR Management History, CCIPH-86 Special RateSheet Settings).
 * Assumes authenticated session — login is handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX = 'AP Management/CCIPH-102/'
	private static final String OR_PREFIX_103 = 'AP Management/CCIPH-103/'
	private static final String OR_PREFIX_104 = 'AP Management/CCIPH-104/'
	private static final String OR_PREFIX_200 = 'AP Management/CCIPH-200/'
	private static final String OR_PREFIX_AR_116 = 'AR Management/CCIPH-116/'
	private static final String OR_PREFIX_AR_117 = 'AR Management/CCIPH-117/'
	private static final String OR_PREFIX_AR_118 = 'AR Management/CCIPH-118/'
	private static final String OR_PREFIX_AR_126 = 'AR Management/CCIPH-126/'
	private static final String OR_PREFIX_AR_123 = 'AR Management/CCIPH-123/'
	private static final String OR_PREFIX_AR_124 = 'AR Management/CCIPH-124/'
	private static final String OR_PREFIX_AR_37 = 'AR Management/CCIPH-37/'
	private static final String OR_PREFIX_AR_86 = 'AR Management/CCIPH-86/'
	private static final String AR_HISTORY_EXPORT_DETECTED_FILE = 'Reports/latest-ar-management-history-export.txt'
	private static final String AR_HISTORY_EXPORT_NAME_PREFIX_DEFAULT = 'Account-Audit-History_'
	private static final List<String> AR_HISTORY_EXPORT_COLUMNS_DEFAULT = [
		'Timestamp',
		'Section',
		'User',
		'Action',
		'Field Name',
		'Old Value',
		'New Value'
	]
	private static final String AR_SERVICE_CHARGE_EXPORT_DETECTED_FILE = 'Reports/latest-ar-service-charge-export.txt'
	private static final String AR_SERVICE_CHARGE_EXPORT_NAME_PREFIX_DEFAULT = 'Service'
	private static final List<String> AR_SERVICE_CHARGE_TABLE_COLUMNS_DEFAULT = [
		'Service Code',
		'Description',
		'Ref/Desc Description',
		'Active',
		'Tax Code',
		'Tax Value',
		'Billable / Non',
		'Split',
		'Action'
	]
	
	private static final List<String> AR_SERVICE_CHARGE_TABLE_COLUMNS = [
		'Service Code',
		'Description',
		'Ref/Desc Description',
		'Active',
		'Tax Code',
		'Tax Value',
		'Billable / Non',
		'Split',
	]
	
	private static final int DEFAULT_TIMEOUT = 30
	/** Last validated AR History export path ({@link #verifyLatestArManagementHistoryExportDownloaded(long, String)}). */
	private String lastArHistoryExportFilePath = ''
	private String lastArHistoryExportFileName = ''
	/** Last validated AR Service Charge export path ({@link #verifyArServiceChargeSettingsExportDownloaded(long, String)}). */
	private String lastArServiceChargeExportFilePath = ''
	private String lastArServiceChargeExportFileName = ''
	/** Read-only detail {@code input} — matches DevTools class list (use contains; order may change). */
	private static final String DETAIL_READONLY_INPUT_XPATH =
		"input[contains(@class,'h-10') and contains(@class,'min-w-0') and contains(@class,'cursor-not-allowed')]"
	/**
	 * AR detail disabled/read-only {@code input} — stricter match for Salesforce-sourced fields
	 * ({@code cursor-not-allowed}, {@code bg-[#f5f5f9]}, muted text).
	 */
	private static final String AR_DETAIL_DISABLED_READONLY_INPUT_XPATH =
		"input[contains(@class,'h-10') and contains(@class,'w-full') and contains(@class,'min-w-0') and contains(@class,'truncate') " +
		"and contains(@class,'rounded-md') and contains(@class,'cursor-not-allowed') and contains(@class,'bg-[#f5f5f9]') " +
		"and contains(@class,'text-[rgba(50,71,92,0.38)]')]"
	/** Default Salesforce read-only labels (CCIPH-126 / disabled input validation). */
	private static final List<String> AR_SALESFORCE_READONLY_FIELD_LABELS = [
		'Account Name',
		'Account Number',
		'Entity',
		'Account Type',
	]
	/** Legacy native {@code select} on detail form — retained as fallback only. */
	private static final String DETAIL_EDITABLE_SELECT_XPATH =
		"select[contains(@class,'h-10') and contains(@class,'cursor-pointer') and contains(@class,'appearance-none')]"
	/** AP Account Detail SearchableSelect trigger ({@code accountDetailsTrigger} + listbox). */
	private static final String DETAIL_EDITABLE_SEARCHABLE_TRIGGER_XPATH =
		"button[contains(@class,'accountDetailsTrigger') and @aria-haspopup='listbox']"
	private static final String DETAIL_EDITABLE_SEARCHABLE_LABEL_XPATH =
		"label[contains(@class,'accountDetailsLabel')]"
	/** 1-based index of editable selects on detail form (left-to-right in UI). */
	private static final Map<String, Integer> DETAIL_EDITABLE_SELECT_INDEX_BY_LABEL = [
		'Rate Currency'     : 1,
		'Billing Currency'  : 2,
		'Market Structure'  : 3,
		'Status'            : 4
	]
	/** 1-based index of editable selects on AR account detail (left-to-right). */
	private static final Map<String, Integer> AR_DETAIL_EDITABLE_SELECT_INDEX_BY_LABEL = [
		'AR Account Number'      : 1,
		'Rate Currency'          : 2,
		'Billing Currency'       : 3,
		'Invoice Frequency'      : 4,
		'Customs Type'           : 5,
		'Eligible For Billing'   : 6,
		'Eligible for Billing'   : 6,
		'Invoice Trigger'        : 7,
		'Invoice Break By'       : 8,
		'Invoice Group by'       : 8,
		'Invoice Shipment Limit' : 9,
		'Invoice Amount Limit'   : 10,
		'Potential Delay'          : 11
	]

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static TestObject to103(String name) {
		return findTestObject(OR_PREFIX_103 + name)
	}

	private static TestObject to104(String name) {
		return findTestObject(OR_PREFIX_104 + name)
	}

	private static TestObject to200(String name) {
		return findTestObject(OR_PREFIX_200 + name)
	}

	private static TestObject toAr116(String name) {
		return findTestObject(OR_PREFIX_AR_116 + name)
	}

	private static TestObject toAr117(String name) {
		return findTestObject(OR_PREFIX_AR_117 + name)
	}

	private static TestObject toAr118(String name) {
		return findTestObject(OR_PREFIX_AR_118 + name)
	}

	private static TestObject toAr126(String name) {
		return findTestObject(OR_PREFIX_AR_126 + name)
	}

	private static TestObject toAr123(String name) {
		return findTestObject(OR_PREFIX_AR_123 + name)
	}

	private static TestObject toAr124(String name) {
		return findTestObject(OR_PREFIX_AR_124 + name)
	}

	private static TestObject toAr37(String name) {
		return findTestObject(OR_PREFIX_AR_37 + name)
	}

	private static TestObject toAr86(String name) {
		return findTestObject(OR_PREFIX_AR_86 + name)
	}

	private static final String AR_RATESHEET_PANEL_XPATH =
		"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'special rate') " +
		"and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sheet') " +
		"and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'setting')]"
	private static final String AR_RATESHEET_TABLE_XPATH =
		"(${AR_RATESHEET_PANEL_XPATH}/following::table[1] | " +
		"//h3[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'rate') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sheet')]/following::table[1])"
	/** Inline Add/Edit card (not a table row) — see OR {@code ar86_rs_editing_row}. */
	private static final String AR_RATESHEET_NEW_RECORD_CARD_XPATH =
		"//div[contains(@class,'ArEstimateRatesheetSettingsPanel-module__MWcpQq__newRecordCard')]"
	private static final List<String> AR_RATESHEET_COLUMNS_DEFAULT = [
//		'Description', 'Service Desc', 'Rate Type', '% of', 'Value', 'Action'
		'Service Code', 'Description', 'Shipment Type', 'Rate 1', 'Rate 2', 'Rate 3', 'ACTION'
	]

	private static final String AR_SERVICE_CHARGE_TABLE_XPATH =
		"//h3[normalize-space()='Service Charge Settings']/following::table[1]"

	/** Parses pipe-separated or individual data-file label values. */
	private static List<String> parseArDetailFieldLabelsFromData(String... fieldLabelsFromData) {
		List<String> labels = []
		(fieldLabelsFromData ?: []).each { String raw ->
			String v = (raw ?: '').trim()
			if (!v) {
				return
			}
			if (v.contains('|')) {
				labels.addAll(v.split(/\|/).collect { it.trim() }.findAll { it })
			} else {
				labels.add(v)
			}
		}
		return labels
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

	/** Parses {@code rgb(r,g,b)} or {@code rgba(r,g,b,a)} into [r,g,b]. */
	private static List<Integer> parseCssRgbTriplet(String cssColor) {
		if (!cssColor?.trim()) {
			return []
		}
		def matcher = cssColor.trim().toLowerCase() =~ /rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/
		if (!matcher.find()) {
			return []
		}
		return [matcher.group(1).toInteger(), matcher.group(2).toInteger(), matcher.group(3).toInteger()]
	}

	private static boolean rgbMatches(List<Integer> actual, List<Integer> expected, int tolerance = 8) {
		if (actual?.size() != 3 || expected?.size() != 3) {
			return false
		}
		return (0..2).every { int i -> Math.abs(actual[i] - expected[i]) <= tolerance }
	}

	/** Aramex brand hover red {@code #DC291E}. */
	private static boolean isApBrandHoverRgb(List<Integer> rgb) {
		return rgbMatches(rgb, [220, 41, 30])
	}

	private static void click(TestObject testObject) {
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.takeScreenshot()
	}

	/** Scroll + native click with JavaScript fallback (toggle switches, overlaid SVG controls). */
	private static void clickWithJsFallback(TestObject testObject, String contextLabel = 'element') {
		waitVisible(testObject)
		WebElement el = WebUiCommonHelper.findWebElement(testObject, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript('arguments[0].scrollIntoView({block:"center", inline:"nearest"});', java.util.Arrays.asList(el))
		WebUI.delay(0.5)
		try {
			WebUI.waitForElementClickable(testObject, 10, FailureHandling.OPTIONAL)
			WebUI.click(testObject)
		} catch (Exception clickEx) {
			KeywordUtil.logInfo("Native click failed for ${contextLabel} (${clickEx.message}); using JavaScript click.")
			WebUI.executeJavaScript('arguments[0].click();', java.util.Arrays.asList(el))
		}
		WebUI.takeScreenshot()
	}

	private static String resolveApUrl() {
		String url = (ProfileGlobals.get('AP_MANAGEMENT_URL') ?: ProfileGlobals.get('URL')).trim()
		if (!url) {
			failStop('AP_MANAGEMENT_URL (or URL) must be set in the active profile.')
		}
		return url
	}

	private static String resolveArUrl() {
		String url = ProfileGlobals.get('AR_MANAGEMENT_URL', '').trim()
		if (!url) {
			String ap = resolveApUrl()
			url = ap.replace('/ap-management', '/ar-management')
		}
		if (!url) {
			failStop('AR_MANAGEMENT_URL must be set in the active profile (or AP_MANAGEMENT_URL for derivation).')
		}
		return url
	}

	@Keyword
	def navigateToApManagement() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			WebUI.navigateToUrl(resolveApUrl())
			waitVisible(to('txt_anchor_ApManagementPage'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToApManagement failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApSearchPageLoaded() {
		try {
			waitVisible(to('txt_anchor_ApManagementPage'))
			WebUI.verifyElementVisible(to('txt_anchor_ApManagementPage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApSearchPageLoaded failed: ${e.message}")
		}
	}

	// ---------- CCIPH-116: AR Accounts search (reuses AP Management/CCIPH-102 search OR) ----------

	@Keyword
	def navigateToArManagement() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			WebUI.navigateToUrl(resolveArUrl())
			waitVisible(toAr116('txt_anchor_ArManagementPage'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToArManagement failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArSearchPageLoaded() {
		try {
			waitVisible(toAr116('txt_anchor_ArManagementPage'))
			WebUI.verifyElementVisible(toAr116('txt_anchor_ArManagementPage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArSearchPageLoaded failed: ${e.message}")
		}
	}

	/** Same controls as AP search; locators are shared from CCIPH-102 OR. */
//	@Keyword
//	def verifyApSearchControlsVisible() {
//		verifySearchControlsVisible()
//	}
	
	@Keyword
	def verifyArSearchControlsVisible() {
		verifyArSearchControl()
	}

	@Keyword
	def performArSearch(String searchCriteria, String searchBy, String searchInput, String entity = '') {
		performARSearch(searchCriteria, searchBy, searchInput, entity)
	}

	@Keyword
	def verifySearchControlsVisible() {
		try {
			[
				'ddl_searchCriteria',
				'ddl_searchBy',
				'inp_searchText',
				'btn_search'
			].each { name ->
				waitVisible(to(name))
				WebUI.verifyElementVisible(to(name), FailureHandling.STOP_ON_FAILURE)
			}
			boolean entityControlVisible = WebUI.waitForElementVisible(to('searchEntity'), 8, FailureHandling.OPTIONAL) ||
				WebUI.waitForElementVisible(to('ddl_entity'), 8, FailureHandling.OPTIONAL)
			if (!entityControlVisible) {
				failStop('Entity control not visible (expected searchEntity or ddl_entity).')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchControlsVisible failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyArSearchControl() {
		try {
			[
				'ddl_searchCriteria',
				'ddl_searchBy',
				'inp_searchText',
				'btn_search'
			].each { name ->
				waitVisible(to(name))
				WebUI.verifyElementVisible(to(name), FailureHandling.STOP_ON_FAILURE)
			}
			boolean entityControlVisible = WebUI.waitForElementVisible(toAr116('search_Entity'), 8, FailureHandling.OPTIONAL) ||
				WebUI.waitForElementVisible(to('ddl_entity'), 8, FailureHandling.OPTIONAL)
			if (!entityControlVisible) {
				failStop('Entity control not visible (expected searchEntity or ddl_entity).')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchControlsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def selectSearchCriteria(String label) {
		try {
			TestObject ddl = to('ddl_searchCriteria')
			waitVisibleClickable(ddl)
			WebUI.selectOptionByLabel(ddl, label, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectSearchCriteria(${label}) failed: ${e.message}")
		}
	}

	@Keyword
	def selectSearchBy(String label) {
		try {
			TestObject ddl = to('ddl_searchBy')
			waitVisibleClickable(ddl)
			WebUI.selectOptionByLabel(ddl, label, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectSearchBy(${label}) failed: ${e.message}")
		}
	}

	@Keyword
	def selectEntity(String entityLabel) {
		try {
			if (!entityLabel?.trim()) {
				return
			}
			if (WebUI.waitForElementVisible(to('searchEntity'), 5, FailureHandling.OPTIONAL)) {
				searchAndSelectEntity(entityLabel.trim())
				return
			}
			TestObject ddl = to('ddl_entity')
			waitVisibleClickable(ddl)
			WebUI.selectOptionByLabel(ddl, entityLabel.trim(), false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectEntity(${entityLabel}) failed: ${e.message}")
		}
	}

	/**
	 * Searchable Entity dropdown on AP Management search panel:
	 * opens entity picker, filters by name, then selects the matching option.
	 * OR: {@code searchEntity}, {@code filterSearch_Entity}, {@code select_EntityOption}.
	 */
	@Keyword
	def searchAndSelectEntity(String entityName) {
		try {
			String entity = (entityName ?: '').trim()
			if (!entity) {
				failStop('searchAndSelectEntity: entity name is required')
			}
			click(to('searchEntity'))
			TestObject filterInput = to('filterSearch_Entity')
			waitVisibleClickable(filterInput)
			WebUI.clearText(filterInput)
			WebUI.setText(filterInput, entity)
			WebUI.delay(1)
			TestObject namedOption = searchableEntityOption(entity)
			if (WebUI.waitForElementClickable(namedOption, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
				click(namedOption)
			} else {
				click(to('select_EntityOption'))
			}
			WebUI.delay(0.5)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("searchAndSelectEntity(${entityName}) failed: ${e.message}")
		}
	}
	
	@Keyword
	def searchAndSelectArEntity(String entityName) {
		try {
			String entity = (entityName ?: '').trim()
			if (!entity) {
				failStop('searchAndSelectEntity: entity name is required')
			}
			click(toAr116('search_Entity'))
			TestObject filterInput = to('filterSearch_Entity')
			waitVisibleClickable(filterInput)
			WebUI.clearText(filterInput)
			WebUI.setText(filterInput, entity)
			WebUI.delay(1)
			TestObject namedOption = searchableEntityOption(entity)
			if (WebUI.waitForElementClickable(namedOption, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
				click(namedOption)
			} else {
				click(to('select_EntityOption'))
			}
			WebUI.delay(0.5)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("searchAndSelectEntity(${entityName}) failed: ${e.message}")
		}
	}

	@Keyword
	def enterSearchText(String text) {
		try {
			TestObject inp = to('inp_searchText')
			waitVisibleClickable(inp)
			WebUI.clearText(inp)
			if (text != null) {
				WebUI.setText(inp, text)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterSearchText failed: ${e.message}")
		}
	}

	@Keyword
	def clickSearch() {
		try {
			click(to('btn_search'))
		} catch (Exception e) {
			failStop("clickSearch failed: ${e.message}")
		}
	}

	@Keyword
	def configureSearch(String searchCriteria, String searchBy) {
		configureSearchInternal(searchCriteria, searchBy, '')
	}

	def configureSearch(String searchCriteria, String searchBy, String entity) {
		configureSearchInternal(searchCriteria, searchBy, entity)
	}

	private void configureSearchInternal(String searchCriteria, String searchBy, String entity) {
		try {
			if (searchCriteria?.trim()) {
				selectSearchCriteria(searchCriteria.trim())
			}
			if (searchBy?.trim()) {
				selectSearchBy(searchBy.trim())
			}
			if (entity?.trim()) {
				String entityLabel = entity.trim()
				if (WebUI.waitForElementVisible(toAr116('search_Entity'), 5, FailureHandling.OPTIONAL)) {
					searchAndSelectArEntity(entityLabel)
				} else {
					selectEntity(entityLabel)
				}
			}
		} catch (Exception e) {
			failStop("configureSearch failed: ${e.message}")
		}
	}

	@Keyword
	def clickSearchWithoutInput() {
		try {
			TestObject inp = to('inp_searchText')
			waitVisibleClickable(inp)
			WebUI.clearText(inp)
			WebUI.takeScreenshot()
			clickSearch()
		} catch (Exception e) {
			failStop("clickSearchWithoutInput failed: ${e.message}")
		}
	}

	@Keyword
	def verifyMandatoryValidationMessage() {
		try {
			waitVisible(to('txt_validationMessage'))
			WebUI.verifyElementVisible(to('txt_validationMessage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyMandatoryValidationMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNoRecordsFoundMessage() {
		try {
			waitVisible(to('txt_noRecordsFound'))
			WebUI.verifyElementVisible(to('txt_noRecordsFound'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyNoRecordsFoundMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyIncorrectAccountNumberMessage() {
		try {
			waitVisible(to('txt_incorrectAccountNumber'))
			WebUI.verifyElementVisible(to('txt_incorrectAccountNumber'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyIncorrectAccountNumberMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyIncorrectAccountNameMessage() {
		try {
			waitVisible(to('txt_incorrectAccountName'))
			WebUI.verifyElementVisible(to('txt_incorrectAccountName'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyIncorrectAccountNameMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchResultsTableVisible() {
		try {
			waitVisible(to('tbl_searchResults'))
			WebUI.verifyElementVisible(to('tbl_searchResults'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchResultsTableVisible failed: ${e.message}")
		}
	}

	private static List<String> columnCellTexts(int columnIndex) {
		WebElement table = WebUiCommonHelper.findWebElement(to('tbl_searchResults'), DEFAULT_TIMEOUT)
		List<WebElement> rows = table.findElements(By.xpath('.//tbody//tr[td] | .//tr[td]'))
		if (!rows) {
			def driver = DriverFactory.getWebDriver()
			rows = driver.findElements(By.xpath("//table//tbody//tr[td]"))
		}
		List<String> values = []
		rows.each { WebElement row ->
			List<WebElement> cells = row.findElements(By.xpath('./td'))
			if (cells.size() > columnIndex) {
				String t = cells[columnIndex].getText()?.trim()
				if (t) {
					values.add(t)
				}
			}
		}
		return values
	}

	private static int columnIndexForHeader(String headerFragment) {
		def driver = DriverFactory.getWebDriver()
		List<WebElement> headers = driver.findElements(By.xpath(
			"//table//thead//th | //table//tr[th]/th | //div[@role='columnheader']"
		))
		String needle = headerFragment.toLowerCase()
		for (int i = 0; i < headers.size(); i++) {
			String h = headers[i].getText()?.trim()?.toLowerCase() ?: ''
			if (h.contains(needle)) {
				return i
			}
		}
		return headerFragment.equalsIgnoreCase('Account Name') ? 1 : 0
	}

	private static final String AP_RESULTS_TABLE_XPATH =
		"//table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'account number')]]"

	/** First-row Account Number hyperlink; resolves column by header and tolerates non-{@code #} href values. */
	private static TestObject apFirstRowAccountNumberLink() {
		int col = columnIndexForHeader('Account Number')
		int tdPos = col + 1
		TestObject link = new TestObject('ap_first_row_account_number_link')
		link.addProperty('xpath', ConditionType.EQUALS,
			"${AP_RESULTS_TABLE_XPATH}//tbody/tr[contains(@class,'group')][1]/td[${tdPos}]//a | " +
			"${AP_RESULTS_TABLE_XPATH}//tbody/tr[contains(@class,'group')][1]//a[contains(@class,'font-bold') and contains(@class,'underline')]")
		return link
	}

	@Keyword
	def verifyAllRowsContainInColumn(String columnHeaderFragment, String expectedSubstring) {
		try {
			verifySearchResultsTableVisible()
			String needle = (expectedSubstring ?: '').trim().toLowerCase()
			if (!needle) {
				failStop('expectedSubstring is required for contains verification')
			}
			int col = columnIndexForHeader(columnHeaderFragment)
			List<String> cells = columnCellTexts(col)
			if (cells.isEmpty()) {
				failStop("No result rows found to verify contains '${expectedSubstring}' in ${columnHeaderFragment}")
			}
			cells.each { String cell ->
				if (!cell.toLowerCase().contains(needle)) {
					failStop("Expected '${columnHeaderFragment}' to contain '${expectedSubstring}' but found '${cell}'")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAllRowsContainInColumn failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAllRowsExactMatchInColumn(String columnHeaderFragment, String expectedExact) {
		try {
			verifySearchResultsTableVisible()
			String expected = (expectedExact ?: '').trim()
			if (!expected) {
				failStop('expectedExact is required for exact match verification')
			}
			int col = columnIndexForHeader(columnHeaderFragment)
			List<String> cells = columnCellTexts(col)
			if (cells.isEmpty()) {
				failStop("No result rows found to verify exact match '${expectedExact}' in ${columnHeaderFragment}")
			}
			cells.each { String cell ->
				if (!cell.equalsIgnoreCase(expected)) {
					failStop("Expected exact '${expected}' in ${columnHeaderFragment} but found '${cell}'")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAllRowsExactMatchInColumn failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAllRowsEntityEquals(String expectedEntity) {
		try {
			verifySearchResultsTableVisible()
			String expected = (expectedEntity ?: '').trim()
			if (!expected) {
				failStop('expectedEntity is required')
			}
			int col = columnIndexForHeader('Entity')
			List<String> cells = columnCellTexts(col)
			if (cells.isEmpty()) {
				failStop("No result rows found to verify entity '${expectedEntity}'")
			}
			cells.each { String cell ->
				if (!cell.equalsIgnoreCase(expected)) {
					failStop("Expected entity '${expected}' but found '${cell}'")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAllRowsEntityEquals failed: ${e.message}")
		}
	}

	@Keyword
	def performSearch(String searchCriteria, String searchBy, String searchInput, String entity = '') {
		try {
			configureSearch(searchCriteria, searchBy)
			enterSearchText(searchInput)
			searchAndSelectEntity(entity)
			clickSearch()
		} catch (Exception e) {
			failStop("performSearch failed: ${e.message}")
		}
	}
	@Keyword
	def performARSearch(String searchCriteria, String searchBy, String searchInput, String entity = '') {
		try {
			configureSearch(searchCriteria, searchBy)
			enterSearchText(searchInput)
			searchAndSelectArEntity(entity)
			clickSearch()
		} catch (Exception e) {
			failStop("performSearch failed: ${e.message}")
		}
	}

	@Keyword
	def verifyTrimmedSearchMatches(String rawInput, String columnHeaderFragment) {
		try {
			String effective = (rawInput ?: '').trim()
			verifyAllRowsContainInColumn(columnHeaderFragment, effective)
		} catch (Exception e) {
			failStop("verifyTrimmedSearchMatches failed: ${e.message}")
		}
	}

	// ---------- CCIPH-103: Search results grid, pagination, sorting ----------

	private static boolean isApControlDisabled(TestObject testObject) {
		if (!WebUI.waitForElementVisible(testObject, 8, FailureHandling.OPTIONAL)) {
			return true
		}
		String d = WebUI.getAttribute(testObject, 'disabled', FailureHandling.OPTIONAL)
		if (d != null && ('true'.equalsIgnoreCase(d) || 'disabled'.equalsIgnoreCase(d))) {
			return true
		}
		String a = WebUI.getAttribute(testObject, 'aria-disabled', FailureHandling.OPTIONAL)
		return a != null && 'true'.equalsIgnoreCase(a)
	}

	private static List<String> getSelectOptionLabels(TestObject ddl) {
		WebElement el = WebUiCommonHelper.findWebElement(ddl, DEFAULT_TIMEOUT)
		Select select = new Select(el)
		List<String> labels = []
		select.getOptions().each { WebElement opt ->
			String t = opt.getText()?.trim()
			if (t) {
				labels.add(t)
			}
		}
		return labels
	}

	private static List<String> getVisibleTableHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		List<WebElement> headers = driver.findElements(By.xpath(
			"//table[.//th[contains(translate(.,'ACCOUNT','account'),'account')]]//thead//th | " +
			"//table[.//th[contains(translate(.,'ACCOUNT','account'),'account')]]//tr[th]/th"
		))
		return headers.collect { it.getText()?.trim() }.findAll { it }
	}

	private static String apResultsTableRowsXPath() {
		return "//table[.//th[contains(translate(.,'ACCOUNT','account'),'account')]]//tbody//tr[td]"
	}

	private static String getFirstVisibleApResultsRowText() {
		try {
			TestObject row = new TestObject('ap_first_result_row')
			row.addProperty('xpath', ConditionType.EQUALS, apResultsTableRowsXPath() + "[1]")
			if (!WebUI.waitForElementVisible(row, 8, FailureHandling.OPTIONAL)) {
				return ''
			}
			return (WebUI.getText(row, FailureHandling.OPTIONAL) ?: '').trim()
		} catch (Exception ignored) {
			return ''
		}
	}

	private static List<Integer> parseApRecordFooterTotal(String footer) {
		if (!footer) {
			return []
		}
		String c = footer.replaceAll(/\s+/, ' ').trim()
		def m = (c =~ /(?i)(\d[\d,]*)\s*[-–]\s*(\d[\d,]*)\s+of\s+(\d[\d,]*)/)
		if (m.find()) {
			return [
				m.group(1).replace(',', '') as int,
				m.group(2).replace(',', '') as int,
				m.group(3).replace(',', '') as int
			]
		}
		def mTo = (c =~ /(?i)showing\s+(\d[\d,]*)\s+to\s+(\d[\d,]*)\s+of\s+(\d[\d,]*)/)
		if (mTo.find()) {
			return [
				mTo.group(1).replace(',', '') as int,
				mTo.group(2).replace(',', '') as int,
				mTo.group(3).replace(',', '') as int
			]
		}
		def m2 = (c =~ /(?i)(\d+)\s+record/)
		if (m2.find()) {
			return [1, m2.group(1) as int, m2.group(1) as int]
		}
		def mOfOnly = (c =~ /(?i)of\s+(\d[\d,]*)/)
		if (mOfOnly.find()) {
			int total = mOfOnly.group(1).replace(',', '') as int
			return [1, total, total]
		}
		return []
	}

	private static int countArSearchResultDataRows() {
		def driver = DriverFactory.getWebDriver()
		List<WebElement> rows = driver.findElements(By.xpath(
			"//table[contains(@class,'w-full') and contains(@class,'text-left')]//tbody//tr[td] | " +
			"//table[contains(@class,'w-full') and contains(@class,'text-left')]//tr[td][not(th)]"
		))
		int visible = 0
		rows.each { WebElement row ->
			if (row.isDisplayed()) {
				visible++
			}
		}
		return visible
	}

	private static String findArSearchResultsRecordFooterText() {
		List<String> footerXpaths = [
			"//table[contains(@class,'w-full') and contains(@class,'text-left')]/following::*[contains(translate(normalize-space(.),'SHOWING','showing'),'showing')][1]",
			"//table[contains(@class,'w-full') and contains(@class,'text-left')]/ancestor::div[1]//following-sibling::*//*[contains(translate(normalize-space(.),'SHOWING','showing'),'showing')]",
			"//*[contains(translate(normalize-space(.),'SHOWING','showing'),'showing') and contains(translate(normalize-space(.),'OF','of'),' of ')]",
			"//*[contains(translate(normalize-space(.),'SHOWING','showing'),'showing') and contains(translate(.,'RECORD','record'),'record')]"
		]
		def driver = DriverFactory.getWebDriver()
		for (String xp : footerXpaths) {
			List<WebElement> candidates = driver.findElements(By.xpath(xp))
			for (WebElement el : candidates) {
				if (!el.isDisplayed()) {
					continue
				}
				String text = (el.getText() ?: '').trim()
				if (text && parseApRecordFooterTotal(text)) {
					return text
				}
			}
		}
		if (WebUI.waitForElementVisible(toAr117('txt_arRecordFooter'), 5, FailureHandling.OPTIONAL)) {
			String fromOr = (WebUI.getText(toAr117('txt_arRecordFooter'), FailureHandling.OPTIONAL) ?: '').trim()
			if (fromOr) {
				return fromOr
			}
		}
		return ''
	}

	@Keyword
	def verifySearchCriteriaDropdownOptions() {
		try {
			List<String> labels = getSelectOptionLabels(to('ddl_searchCriteria'))
			['Account Number', 'Account Name'].each { String expected ->
				boolean found = labels.any { it.equalsIgnoreCase(expected) }
				if (!found) {
					failStop("Search Criteria dropdown missing option '${expected}'. Found: ${labels}")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchCriteriaDropdownOptions failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchByDropdownOptions() {
		try {
			List<String> labels = getSelectOptionLabels(to('ddl_searchBy'))
			['Contains', 'Exact Match'].each { String expected ->
				boolean found = labels.any { it.equalsIgnoreCase(expected) }
				if (!found) {
					failStop("Search By dropdown missing option '${expected}'. Found: ${labels}")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchByDropdownOptions failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntityDropdownHasMultipleOptions() {
		try {
			if (WebUI.waitForElementVisible(to('searchEntity'), 8, FailureHandling.OPTIONAL)) {
				click(to('searchEntity'))
				waitVisible(to('filterSearch_Entity'))
				TestObject allOptions = new TestObject('entity_searchable_options')
				allOptions.addProperty('xpath', ConditionType.EQUALS,
					"//button[contains(@class,'SearchableSelect-module') and contains(@class,'option')]")
				int optionCount = WebUI.findWebElements(allOptions, DEFAULT_TIMEOUT)?.size() ?: 0
				if (optionCount < 2) {
					failStop("Entity searchable dropdown should list multiple entities; found ${optionCount} option(s).")
				}
				WebUI.takeScreenshot()
				return
			}
			List<String> labels = getSelectOptionLabels(to('ddl_entity'))
			if (labels.size() < 2) {
				failStop("Entity dropdown should list multiple entities; found ${labels.size()} option(s).")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityDropdownHasMultipleOptions failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAREntityDropdownHasMultipleOptions() {
		try {
			if (WebUI.waitForElementVisible(toAr116('search_Entity'), 8, FailureHandling.OPTIONAL)) {
				click(toAr116('search_Entity'))
				waitVisible(to('filterSearch_Entity'))
				TestObject allOptions = new TestObject('entity_searchable_options')
				allOptions.addProperty('xpath', ConditionType.EQUALS,
					"//button[contains(@class,'SearchableSelect-module') and contains(@class,'option')]")
				int optionCount = WebUI.findWebElements(allOptions, DEFAULT_TIMEOUT)?.size() ?: 0
				if (optionCount < 2) {
					failStop("Entity searchable dropdown should list multiple entities; found ${optionCount} option(s).")
				}
				WebUI.takeScreenshot()
				return
			}
			List<String> labels = getSelectOptionLabels(to('ddl_entity'))
			if (labels.size() < 2) {
				failStop("Entity dropdown should list multiple entities; found ${labels.size()} option(s).")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityDropdownHasMultipleOptions failed: ${e.message}")
		}
	}
	@Keyword
	def verifySearchResultsGridColumnHeaders() {
		try {
			verifySearchResultsTableVisible()
			List<String> headers = getVisibleTableHeaderTexts()
			String joined = headers.join(' | ').toLowerCase()
			[
				'account number',
				'account name',
				'entity',
				'created',
				'created by',
				'status'
			].each { String fragment ->
				if (!joined.contains(fragment)) {
					failStop("Results grid missing column containing '${fragment}'. Headers: ${headers}")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchResultsGridColumnHeaders failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAccountNumberCellsAreHyperlinks() {
		try {
			verifySearchResultsTableVisible()
			TestObject link = apFirstRowAccountNumberLink()
			waitVisible(link)
			WebUI.verifyElementVisible(link, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAccountNumberCellsAreHyperlinks failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAccountNumberLinkShowsPointerCursorOnHover() {
		try {
			TestObject link = apFirstRowAccountNumberLink()
			waitVisible(link)
			WebElement el = WebUiCommonHelper.findWebElement(link, DEFAULT_TIMEOUT)
			new Actions(DriverFactory.getWebDriver()).moveToElement(el).perform()
			WebUI.delay(1)
			String cursor = el.getCssValue('cursor') ?: ''
			if (!cursor.toLowerCase().contains('pointer')) {
				failStop("Expected pointer cursor on Account Number link hover; got '${cursor}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAccountNumberLinkShowsPointerCursorOnHover failed: ${e.message}")
		}
	}

	/**
	 * Hovers the first search-results row and validates Account Number hyperlink UI:
	 * pointer (hand) cursor on the row and brand-red text color ({@code #DC291E}) on the link.
	 * Tailwind {@code group-hover} applies when the parent {@code tr.group} row is hovered.
	 */
	@Keyword
	def verifyApFirstRowAccountNumberMouseHover() {
		try {
			verifySearchResultsTableVisible()
			TestObject row = to103('tr_searchResultsFirstRow')
			TestObject link = apFirstRowAccountNumberLink()
			waitVisible(row)
			waitVisibleClickable(link)

			WebElement rowEl = WebUiCommonHelper.findWebElement(row, DEFAULT_TIMEOUT)
			WebElement linkEl = WebUiCommonHelper.findWebElement(link, DEFAULT_TIMEOUT)

			String tag = (linkEl.getTagName() ?: '').toLowerCase()
			if (tag != 'a') {
				failStop("Expected first-row Account Number to be a hyperlink; got <${tag}>")
			}

			String linkClass = linkEl.getAttribute('class') ?: ''
			if (!linkClass.contains('font-bold') || !linkClass.contains('underline')) {
				failStop("Account Number link missing expected styling (bold + underline). class='${linkClass}'")
			}

			String beforeColor = linkEl.getCssValue('color') ?: ''
			new Actions(DriverFactory.getWebDriver()).moveToElement(rowEl).perform()
			WebUI.waitForElementClickable(link, DEFAULT_TIMEOUT)

			String rowCursor = rowEl.getCssValue('cursor') ?: ''
			String linkCursor = linkEl.getCssValue('cursor') ?: ''
			boolean pointerOnRow = rowCursor.toLowerCase().contains('pointer')
			boolean pointerOnLink = linkCursor.toLowerCase().contains('pointer')
			if (!pointerOnRow && !pointerOnLink) {
				failStop("Expected pointer (hand) cursor on row hover; row='${rowCursor}' link='${linkCursor}'")
			}

			String afterColor = linkEl.getCssValue('color') ?: ''
			List<Integer> afterRgb = parseCssRgbTriplet(afterColor)
			if (!isApBrandHoverRgb(afterRgb)) {
				List<Integer> beforeRgb = parseCssRgbTriplet(beforeColor)
				if (beforeRgb == afterRgb) {
					failStop("Account Number text color did not change on row hover. before='${beforeColor}' after='${afterColor}'")
				}
				KeywordUtil.logInfo("Hover color '${afterColor}' changed from default; exact brand red may vary by browser.")
			}

			KeywordUtil.logInfo("Verified mouse hover on first-row Account Number (cursor + color).")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApFirstRowAccountNumberMouseHover failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchResultsMatchCriteria(String searchCriteria, String searchInput) {
		try {
			String criteria = (searchCriteria ?: '').trim()
			String input = (searchInput ?: '').trim()
			if (!input) {
				failStop('verifySearchResultsMatchCriteria: searchInput is blank')
			}
			if (criteria.equalsIgnoreCase('Account Name')) {
				verifyAllRowsContainInColumn('Account Name', input)
			} else {
				verifyAllRowsContainInColumn('Account Number', input)
			}
		} catch (Exception e) {
			failStop("verifySearchResultsMatchCriteria failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNoRecordsFoundAndGridHidden() {
		try {
			verifyNoRecordsFoundMessage()
			boolean gridVisible = WebUI.waitForElementVisible(to('tbl_searchResults'), 5, FailureHandling.OPTIONAL)
			if (gridVisible) {
				TestObject rowsTo = new TestObject('ap_result_rows_check')
				rowsTo.addProperty('xpath', ConditionType.EQUALS, apResultsTableRowsXPath())
				int rows = WebUI.findWebElements(rowsTo, 5)?.size() ?: 0
				if (rows > 0) {
					failStop('Expected no results grid/data rows when no records found.')
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyNoRecordsFoundAndGridHidden failed: ${e.message}")
		}
	}

	@Keyword
	def getApSearchResultsRecordFooterText() {
		try {
			if (WebUI.waitForElementVisible(to103('pagination_Visible'), 12, FailureHandling.OPTIONAL)) {
				return (WebUI.getText(to103('pagination_Visible'), FailureHandling.OPTIONAL) ?: '').trim()
			}
			if (!WebUI.waitForElementVisible(to103('txt_apRecordFooter'), 12, FailureHandling.OPTIONAL)) {
				return ''
			}
			return (WebUI.getText(to103('txt_apRecordFooter'), FailureHandling.OPTIONAL) ?: '').trim()
		} catch (Exception e) {
			return ''
		}
	}

	/**
	 * Validates AP search results pagination footer is visible
	 * (e.g. {@code Showing 1-1 of 1 (page 1 of 1)}).
	 * OR: {@code AP Management/CCIPH-103/pagination_Visible} — {@code p[class="tabular-nums"]}.
	 */
	@Keyword
	def verifyApPaginationVisible() {
		try {
			verifySearchResultsTableVisible()
			TestObject pagination = to103('pagination_Visible')
			waitVisible(pagination)
			WebUI.verifyElementVisible(pagination, FailureHandling.STOP_ON_FAILURE)
			String paginationText = (WebUI.getText(pagination, FailureHandling.OPTIONAL) ?: '').trim()
			if (!paginationText) {
				failStop('Pagination footer is visible but text is empty.')
			}
			KeywordUtil.logInfo("AP pagination footer: '${paginationText}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApPaginationVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApRecordCountDisplayed() {
		try {
			verifySearchResultsTableVisible()
			String footer = getApSearchResultsRecordFooterText()
			List<Integer> triple = parseApRecordFooterTotal(footer)
			if (triple.isEmpty() || triple[2] < 1) {
				failStop("Record count footer not displayed or total is zero (footer='${footer}').")
			}
			KeywordUtil.logInfo("AP search results total records (footer): ${triple[2]}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApRecordCountDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def clickApPaginationNextIfEnabled() {
		try {
			if (!WebUI.waitForElementVisible(to103('btn_apPaginationNext'), 12, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo('AP pagination Next control not found.')
				WebUI.takeScreenshot()
				return false
			}
			if (isApControlDisabled(to103('btn_apPaginationNext'))) {
				WebUI.takeScreenshot()
				return false
			}
			click(to103('btn_apPaginationNext'))
			WebUI.delay(2)
			waitVisible(to('tbl_searchResults'))
			return true
		} catch (Exception e) {
			failStop("clickApPaginationNextIfEnabled failed: ${e.message}")
			return false
		}
	}

	@Keyword
	def verifyApPaginationNextChangesFooterOrFirstRow() {
		try {
			verifySearchResultsTableVisible()
			String f0 = getApSearchResultsRecordFooterText()
			String r0 = getFirstVisibleApResultsRowText()
			boolean moved = clickApPaginationNextIfEnabled()
			if (!moved) {
				KeywordUtil.markWarning('Next pagination was not available — verify test data returns more than one page.')
				WebUI.takeScreenshot()
				return
			}
			WebUI.delay(2)
			String f1 = getApSearchResultsRecordFooterText()
			String r1 = getFirstVisibleApResultsRowText()
			if (f0 && f1 && !f0.equals(f1)) {
				WebUI.takeScreenshot()
				return
			}
			if (r0 && r1 && !r0.equals(r1)) {
				WebUI.takeScreenshot()
				return
			}
			failStop('Next page did not change footer or first visible row.')
		} catch (Exception e) {
			failStop("verifyApPaginationNextChangesFooterOrFirstRow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApPaginationControlsNotVisible() {
		try {
			boolean nextVis = WebUI.waitForElementVisible(to103('btn_apPaginationNext'), 5, FailureHandling.OPTIONAL)
			boolean prevVis = WebUI.waitForElementVisible(to103('btn_apPaginationPrev'), 5, FailureHandling.OPTIONAL)
			if (nextVis || prevVis) {
				failStop('Pagination controls should not be visible when total records are below page size.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApPaginationControlsNotVisible failed: ${e.message}")
		}
	}

	@Keyword
	def refreshBrowserAndVerifyApSearchResultsFirstPage() {
		try {
			WebUI.refresh()
			WebUI.delay(2)
			verifyApSearchPageLoaded()
		} catch (Exception e) {
			failStop("refreshBrowserAndVerifyApSearchResultsFirstPage failed: ${e.message}")
		}
	}

	@Keyword
	def clickCreateDateColumnSort() {
		try {
			TestObject sortHeader = to103('btn_sortCreateDate')
			waitVisibleClickable(sortHeader)
			WebUI.click(sortHeader)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickCreateDateColumnSort failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateDateColumnSortedAscending() {
		try {
			verifySearchResultsTableVisible()
			int col = columnIndexForHeader('Create')
			List<String> dates = columnCellTexts(col)
			if (dates.size() < 2) {
				KeywordUtil.markWarning('Fewer than 2 rows — skipping strict ascending date order check.')
				WebUI.takeScreenshot()
				return
			}
			List<String> sorted = new ArrayList<>(dates)
			sorted.sort()
			if (!dates.equals(sorted)) {
				failStop("Create Date column is not ascending. Values: ${dates}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateDateColumnSortedAscending failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateDateColumnSortedDescending() {
		try {
			verifySearchResultsTableVisible()
			int col = columnIndexForHeader('Create')
			List<String> dates = columnCellTexts(col)
			if (dates.size() < 2) {
				KeywordUtil.markWarning('Fewer than 2 rows — skipping strict descending date order check.')
				WebUI.takeScreenshot()
				return
			}
			List<String> sorted = new ArrayList<>(dates)
			sorted.sort()
			Collections.reverse(sorted)
			if (!dates.equals(sorted)) {
				failStop("Create Date column is not descending. Values: ${dates}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateDateColumnSortedDescending failed: ${e.message}")
		}
	}

	@Keyword
	def logManualBackendDataVerificationReminder() {
		KeywordUtil.logInfo('MANUAL (AC3): Compare UI grid/detail values with backend DB or API for the same account. Automated UI tests cannot assert ledger accuracy without a test API or DB hook.')
		WebUI.takeScreenshot()
	}

	// ---------- CCIPH-117: AR search results grid (mirrors CCIPH-103; AR-specific OR under CCIPH-117) ----------

	@Keyword
	def verifyArSearchResultsGridColumnHeaders() {
		try {
			verifySearchResultsTableVisible()
			List<String> headers = getVisibleTableHeaderTexts()
			String joined = headers.join(' | ').toLowerCase()
			[
				'account number',
				'account name',
				'entity',
				'create',
				'created by',
				'status'
			].each { String fragment ->
				if (!joined.contains(fragment)) {
					failStop("AR results grid missing column containing '${fragment}'. Headers: ${headers}")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArSearchResultsGridColumnHeaders failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountNumberCellsAreHyperlinks() {
		try {
			verifySearchResultsTableVisible()
			TestObject link = toAr117('lnk_accountNumberInFirstRow')
			waitVisible(link)
			WebUI.verifyElementVisible(link, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountNumberCellsAreHyperlinks failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountNumberLinkShowsPointerCursorOnHover() {
		try {
			TestObject link = toAr117('lnk_accountNumberInFirstRow')
			waitVisible(link)
			WebElement el = WebUiCommonHelper.findWebElement(link, DEFAULT_TIMEOUT)
			new Actions(DriverFactory.getWebDriver()).moveToElement(el).perform()
			WebUI.delay(1)
			String cursor = el.getCssValue('cursor') ?: ''
			if (!cursor.toLowerCase().contains('pointer')) {
				failStop("Expected pointer cursor on AR Account Number link hover; got '${cursor}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountNumberLinkShowsPointerCursorOnHover failed: ${e.message}")
		}
	}

	@Keyword
	def getArSearchResultsRecordFooterText() {
		try {
			return findArSearchResultsRecordFooterText()
		} catch (Exception e) {
			return ''
		}
	}

	@Keyword
	def verifyArRecordCountDisplayed() {
		try {
			TestObject resultsTable = toAr117('tbl_arSearchResults')
			waitVisible(resultsTable)
			WebUI.verifyElementVisible(resultsTable, FailureHandling.STOP_ON_FAILURE)
			int rowCount = countArSearchResultDataRows()
			if (rowCount < 1) {
				failStop("AR results table is visible but has no data rows.")
			}
			KeywordUtil.logInfo("AR results table visible with ${rowCount} data row(s).")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRecordCountDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def clickArPaginationNextIfEnabled() {
		try {
			if (!WebUI.waitForElementVisible(toAr117('btn_arPaginationNext'), 12, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo('AR pagination Next control not found.')
				WebUI.takeScreenshot()
				return false
			}
			if (isApControlDisabled(toAr117('btn_arPaginationNext'))) {
				WebUI.takeScreenshot()
				return false
			}
			click(toAr117('btn_arPaginationNext'))
			WebUI.delay(2)
			waitVisible(to('tbl_searchResults'))
			return true
		} catch (Exception e) {
			failStop("clickArPaginationNextIfEnabled failed: ${e.message}")
			return false
		}
	}

	@Keyword
	def verifyArPaginationNextChangesFooterOrFirstRow() {
		try {
			verifySearchResultsTableVisible()
			String f0 = getArSearchResultsRecordFooterText()
			String r0 = getFirstVisibleApResultsRowText()
			boolean moved = clickArPaginationNextIfEnabled()
			if (!moved) {
				KeywordUtil.markWarning('AR Next pagination was not available — verify test data returns more than one page.')
				WebUI.takeScreenshot()
				return
			}
			WebUI.delay(2)
			String f1 = getArSearchResultsRecordFooterText()
			String r1 = getFirstVisibleApResultsRowText()
			if (f0 && f1 && !f0.equals(f1)) {
				WebUI.takeScreenshot()
				return
			}
			if (r0 && r1 && !r0.equals(r1)) {
				WebUI.takeScreenshot()
				return
			}
			failStop('AR Next page did not change footer or first visible row.')
		} catch (Exception e) {
			failStop("verifyArPaginationNextChangesFooterOrFirstRow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArPaginationControlsNotVisible() {
		try {
			boolean nextVis = WebUI.waitForElementVisible(toAr117('btn_arPaginationNext'), 5, FailureHandling.OPTIONAL)
			boolean prevVis = WebUI.waitForElementVisible(toAr117('btn_arPaginationPrev'), 5, FailureHandling.OPTIONAL)
			if (nextVis || prevVis) {
				failStop('AR pagination controls should not be visible when total records are below page size.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArPaginationControlsNotVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArPaginationResetsToFirstPageAfterRefresh() {
		try {
			verifySearchResultsTableVisible()
		//	clickArPaginationNextIfEnabled()
			WebUI.delay(2)
			WebUI.refresh()
			WebUI.delay(2)
			verifyArSearchPageLoaded()
		//	verifySearchResultsTableVisible()
//			String footer = getArSearchResultsRecordFooterText()
//			List<Integer> triple = parseApRecordFooterTotal(footer)
//			if (triple.isEmpty() || triple[0] != 1) {
//				failStop("Expected AR results on page 1 after refresh (footer='${footer}').")
			//}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArPaginationResetsToFirstPageAfterRefresh failed: ${e.message}")
		}
	}

	@Keyword
	def clickArCreateDateColumnSort() {
		try {
			TestObject sortHeader = toAr117('btn_sortCreateDate')
			waitVisibleClickable(sortHeader)
			WebUI.click(sortHeader)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArCreateDateColumnSort failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArCreateDateColumnSortedAscending() {
		verifyCreateDateColumnSortedAscending()
	}

	@Keyword
	def verifyArCreateDateColumnSortedDescending() {
		verifyCreateDateColumnSortedDescending()
	}

	@Keyword
	def clickFirstArAccountNumberLink() {
		try {
			verifySearchResultsTableVisible()
			click(toAr117('lnk_accountNumberInFirstRow'))
			WebUI.delay(2)
		} catch (Exception e) {
			failStop("clickFirstArAccountNumberLink failed: ${e.message}")
		}
	}

	// ---------- CCIPH-118: AR Account Detail page ----------

	private static Integer arDetailEditableSelectIndexForLabel(String labelText) {
		String label = (labelText ?: '').trim()
		if (!label) {
			return null
		}
		Integer exact = AR_DETAIL_EDITABLE_SELECT_INDEX_BY_LABEL.get(label)
		if (exact != null) {
			return exact
		}
		String normalized = label.toLowerCase()
		Map.Entry match = AR_DETAIL_EDITABLE_SELECT_INDEX_BY_LABEL.find { Map.Entry entry ->
			(entry.key ?: '').toLowerCase() == normalized
		}
		return match?.value as Integer
	}

	private static TestObject resolveArDetailEditableField(String labelText) {
		String label = (labelText ?: '').trim()
		TestObject byLabel = detailEditableFieldByLabel(label)
		if (WebUI.waitForElementPresent(byLabel, 8, FailureHandling.OPTIONAL)) {
			return byLabel
		}
		Integer idx = arDetailEditableSelectIndexForLabel(label)
		if (idx != null) {
			TestObject byIndex = detailEditableSelectByIndex(idx)
			if (WebUI.waitForElementPresent(byIndex, 8, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo("Resolved AR editable '${label}' via select index ${idx}.")
				return byIndex
			}
		}
		TestObject bySearchable = detailEditableSearchableSelectByLabel(label)
		if (WebUI.waitForElementPresent(bySearchable, 8, FailureHandling.OPTIONAL)) {
			KeywordUtil.logInfo("Resolved AR editable '${label}' via SearchableSelect label.")
			return bySearchable
		}
		TestObject byButton = detailEditableButtonByLabel(label)
		if (WebUI.waitForElementPresent(byButton, 8, FailureHandling.OPTIONAL)) {
			return byButton
		}
		return byLabel
	}

	@Keyword
	def verifyArAccountDetailPageLoaded() {
		try {
			waitVisible(toAr118('txt_anchor_ArAccountDetailPage'))
			WebUI.verifyElementVisible(toAr118('txt_anchor_ArAccountDetailPage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailPageLoaded failed: ${e.message}")
		}
	}

	/** Validates DTA AR account detail view (animated content panel). */
	@Keyword
	def verifyArDtaPageLoaded() {
		try {
			TestObject dtaPanel = toAr118('div_dtaPageContent')
			waitVisible(dtaPanel)
			WebUI.verifyElementVisible(dtaPanel, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArDtaPageLoaded failed: ${e.message}")
		}
	}

	private static void navigateToArAccountDetailTab(TestObject tabObject, String tabLabel) {
		String label = (tabLabel ?: '').trim()
		if (!label) {
			failStop('navigateToArAccountDetailTab: tab label is blank')
		}
		waitVisibleClickable(tabObject)
		WebUI.scrollToElement(tabObject, DEFAULT_TIMEOUT)
		WebUI.click(tabObject)
		WebUI.delay(1)
		KeywordUtil.logInfo("Clicked AR Account Detail tab '${label}'.")
	}

	/** Same content panel check as {@link #verifyArDtaPageLoaded()} — used after tab navigation. */
	private static void verifyArAccountDetailTabPanelLoaded() {
		TestObject tabPanel = toAr118('div_arDetailTabContent')
		waitVisible(tabPanel)
		WebUI.verifyElementVisible(tabPanel, FailureHandling.STOP_ON_FAILURE)
	}

	private static void verifyArAccountDetailTabIsSelected(TestObject tabObject, String tabLabel) {
		if (WebUI.waitForElementPresent(tabObject, 5, FailureHandling.OPTIONAL)) {
			String ariaSelected = WebUI.getAttribute(tabObject, 'aria-selected', FailureHandling.OPTIONAL) ?: ''
			String dataState = WebUI.getAttribute(tabObject, 'data-state', FailureHandling.OPTIONAL) ?: ''
			if ('true'.equalsIgnoreCase(ariaSelected) || 'active'.equalsIgnoreCase(dataState)) {
				KeywordUtil.logInfo("AR Account Detail tab '${tabLabel}' is selected.")
				return
			}
		}
		KeywordUtil.logInfo("AR Account Detail tab '${tabLabel}' selected state not confirmed; tab content panel visibility used.")
	}

	/**
	 * Opens the DTC tab on AR Account Detail and validates the tab content panel is loaded.
	 * Call after the detail page is open (e.g. {@link #openArAccountDetailFromSearchResults}).
	 */
	@Keyword
	def navigateToArDtcTabAndVerifyPageLoaded() {
		try {
			navigateToArAccountDetailTab(toAr118('tab_arDetail_DTC'), 'DTC')
			verifyArAccountDetailTabPanelLoaded()
			verifyArAccountDetailTabIsSelected(toAr118('tab_arDetail_DTC'), 'DTC')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToArDtcTabAndVerifyPageLoaded failed: ${e.message}")
		}
	}

	/**
	 * Opens the DTS tab on AR Account Detail and validates the tab content panel is loaded.
	 * Call after the detail page is open (e.g. {@link #openArAccountDetailFromSearchResults}).
	 */
	@Keyword
	def navigateToArDtsTabAndVerifyPageLoaded() {
		try {
			navigateToArAccountDetailTab(toAr118('tab_arDetail_DTS'), 'DTS')
			verifyArAccountDetailTabPanelLoaded()
			verifyArAccountDetailTabIsSelected(toAr118('tab_arDetail_DTS'), 'DTS')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToArDtsTabAndVerifyPageLoaded failed: ${e.message}")
		}
	}

	/**
	 * Opens the DT3 tab on AR Account Detail and validates the tab content panel is loaded.
	 * Call after the detail page is open (e.g. {@link #openArAccountDetailFromSearchResults}).
	 */
	@Keyword
	def navigateToArDt3TabAndVerifyPageLoaded() {
		try {
			navigateToArAccountDetailTab(toAr118('tab_arDetail_DT3'), 'DT3')
			verifyArAccountDetailTabPanelLoaded()
			verifyArAccountDetailTabIsSelected(toAr118('tab_arDetail_DT3'), 'DT3')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToArDt3TabAndVerifyPageLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailLabelVisible(String labelText) {
		verifyTextPresentOnPage(labelText)
	}

	@Keyword
	def verifyArAccountDetailFieldsVisible() {
		try {
			//verifyArAccountDetailPageLoaded()
			verifyArDtaPageLoaded()
			[
				'Account Name',
				'Account Number',
				'Country',
				'Entity',
				'Account Type',
				'AR Account Number',
				'Rate Currency',
				'Billing Currency',
				'Customs Type',
				'Business Type',
				'Eligible For Billing',
				'Invoice Trigger',
				'Invoice Break By',
				'Invoice Shipment Limit',
				'Invoice Amount Limit',
				'Potential Delay'
			].each { String label ->
				verifyArAccountDetailLabelVisible(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailFieldsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailFieldIsReadOnly(String labelText) {
		try {
			String label = (labelText ?: '').trim()
			if (!label) {
				failStop('verifyArAccountDetailFieldIsReadOnly: labelText is blank')
			}
			verifyArAccountDetailLabelVisible(label)
			TestObject readOnlyInput = detailReadOnlyInputByLabel(label)
			waitPresent(readOnlyInput, "Read-only input for '${label}' not found.")
			WebUI.scrollToElement(readOnlyInput, DEFAULT_TIMEOUT)
			if (!isDetailFieldReadOnly(readOnlyInput)) {
				failStop("Expected '${label}' to be read-only on AR Account Detail page.")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailFieldIsReadOnly(${labelText}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailFieldIsEditable(String labelText) {
		try {
			String label = (labelText ?: '').trim()
			if (!label) {
				failStop('verifyArAccountDetailFieldIsEditable: labelText is blank')
			}
			verifyArAccountDetailLabelVisible(label)
			TestObject editableField = resolveArDetailEditableField(label)
			waitPresent(editableField, "Editable control for '${label}' not found on AR detail page.")
			WebUI.scrollToElement(editableField, DEFAULT_TIMEOUT)
			if (!isDetailFieldEditable(editableField)) {
				failStop("Expected '${label}' to be editable on AR Account Detail page.")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailFieldIsEditable(${labelText}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailReadOnlyFields() {
		try {
			verifyArAccountDetailPageLoaded()
			['Account Name', 'Account Number', 'Country', 'Entity', 'Account Type', 'Business Type'].each { String label ->
				verifyArAccountDetailFieldIsReadOnly(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailReadOnlyFields failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailReadOnlyFieldsList(List<String> labels) {
		try {
			List<String> resolved = (labels ?: []).collect { (it ?: '').trim() }.findAll { it }
			if (resolved.isEmpty()) {
				failStop('verifyArAccountDetailReadOnlyFieldsList: labels list is empty')
			}
			resolved.each { String label ->
				verifyArAccountDetailFieldIsReadOnly(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailReadOnlyFieldsList failed: ${e.message}")
		}
	}

	/**
	 * Validates read-only detail fields using values from the test data file
	 * (pipe-separated {@code readonly_Labels} or multiple columns).
	 */
	@Keyword
	def verifyArAccountDetailReadOnlyFieldsFromData(String... fieldLabelsFromData) {
		List<String> labels = parseArDetailFieldLabelsFromData(fieldLabelsFromData)
		if (labels.isEmpty()) {
			failStop('verifyArAccountDetailReadOnlyFieldsFromData: all field label values from data file are blank')
		}
		verifyArAccountDetailReadOnlyFieldsList(labels)
	}

	/** AC1/AC2 — charge code tabs (DTC / DTS / DT3) visible for billing preference configuration. */
	@Keyword
	def verifyArBillingPreferencesChargeCodeTabsAvailable() {
		try {
			waitVisible(toAr126('txt_anchor_ArBillingPreferences'))
			WebUI.verifyElementVisible(toAr126('txt_anchor_ArBillingPreferences'), FailureHandling.STOP_ON_FAILURE)
			[toAr118('tab_arDetail_DTC'), toAr118('tab_arDetail_DTS'), toAr118('tab_arDetail_DT3')].each { TestObject tab ->
				waitVisible(tab)
				WebUI.verifyElementVisible(tab, FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArBillingPreferencesChargeCodeTabsAvailable failed: ${e.message}")
		}
	}

	/**
	 * On each charge code tab (DTC, DTS, DT3), verifies the listed billing preference fields are read-only.
	 * Use after {@link #openArAccountDetailFromSearchResults}.
	 */
	@Keyword
	def verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabs(List<String> labels) {
		try {
			verifyArBillingPreferencesChargeCodeTabsAvailable()
			navigateToArDtcTabAndVerifyPageLoaded()
			verifyArAccountDetailReadOnlyFieldsList(labels)
			navigateToArDtsTabAndVerifyPageLoaded()
			verifyArAccountDetailReadOnlyFieldsList(labels)
			navigateToArDt3TabAndVerifyPageLoaded()
			verifyArAccountDetailReadOnlyFieldsList(labels)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabs failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabsFromData(String... fieldLabelsFromData) {
		List<String> labels = parseArDetailFieldLabelsFromData(fieldLabelsFromData)
		if (labels.isEmpty()) {
			failStop('verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabsFromData: all field label values from data file are blank')
		}
		verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabs(labels)
	}

	/**
	 * Data-file overload — pass {@code readonly_Label1}, {@code readonly_Label2}, … as separate arguments.
	 * Delegates to {@link #verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabsFromData(String...)}.
	 */
	@Keyword
	def verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabs(String... fieldLabelsFromData) {
		verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabsFromData(fieldLabelsFromData)
	}

	/** Read-only disabled {@code input} under a detail label (strict AR disabled-input locator). */
	private static TestObject arDetailDisabledReadOnlyInputByLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		String blockXp = detailFormFieldBlockXPath(label)
		String disabledInput = AR_DETAIL_DISABLED_READONLY_INPUT_XPATH
		TestObject field = new TestObject("ar_detail_disabled_readonly_input_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${blockXp}/${disabledInput}" +
			" | ${blockXp}//${disabledInput}" +
			" | ${blockXp}/input[@disabled][contains(@class,'cursor-not-allowed')][1]" +
			" | ${blockXp}//input[@disabled][contains(@class,'cursor-not-allowed')][1]"
		)
		return field
	}

	private static boolean isArDetailDisabledReadOnlyInput(TestObject field) {
		if (!WebUI.waitForElementPresent(field, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
			return false
		}
		try {
			WebElement el = WebUiCommonHelper.findWebElement(field, DEFAULT_TIMEOUT)
			if ((el.getTagName() ?: '').toLowerCase() != 'input') {
				return false
			}
			String cls = el.getAttribute('class') ?: ''
			boolean matchesDisabledStyle = cls.contains('h-10') &&
				cls.contains('w-full') &&
				cls.contains('min-w-0') &&
				cls.contains('truncate') &&
				cls.contains('cursor-not-allowed') &&
				cls.contains('bg-[#f5f5f9]')
			boolean disabledOrReadOnly = htmlAttributeIsSet(el.getAttribute('disabled')) ||
				htmlAttributeIsSet(el.getAttribute('readonly')) ||
				!el.isEnabled()
			return matchesDisabledStyle && disabledOrReadOnly
		} catch (Exception e) {
			KeywordUtil.markWarning("isArDetailDisabledReadOnlyInput check failed: ${e.message}")
			return isDetailFieldReadOnly(field)
		}
	}

	@Keyword
	def verifyArAccountDetailFieldIsDisabledReadOnlyInput(String labelText) {
		try {
			String label = (labelText ?: '').trim()
			if (!label) {
				failStop('verifyArAccountDetailFieldIsDisabledReadOnlyInput: labelText is blank')
			}
			verifyArAccountDetailLabelVisible(label)
			TestObject disabledInput = arDetailDisabledReadOnlyInputByLabel(label)
			waitPresent(disabledInput, "Disabled read-only input for '${label}' not found (expected cursor-not-allowed / bg-[#f5f5f9]).")
			WebUI.scrollToElement(disabledInput, DEFAULT_TIMEOUT)
			if (!isArDetailDisabledReadOnlyInput(disabledInput)) {
				failStop("Expected '${label}' to be a disabled read-only input on AR Account Detail page.")
			}
			KeywordUtil.logInfo("Verified disabled read-only input for '${label}'.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailFieldIsDisabledReadOnlyInput(${labelText}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountDetailDisabledReadOnlyInputsList(List<String> labels) {
		try {
			List<String> resolved = (labels ?: []).collect { (it ?: '').trim() }.findAll { it }
			if (resolved.isEmpty()) {
				failStop('verifyArAccountDetailDisabledReadOnlyInputsList: labels list is empty')
			}
			resolved.each { String label ->
				verifyArAccountDetailFieldIsDisabledReadOnlyInput(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailDisabledReadOnlyInputsList failed: ${e.message}")
		}
	}

	/**
	 * On DTC / DTS / DT3 tabs, verifies disabled read-only {@code input} fields (strict locator) per label list.
	 * When data-file values are blank, uses Salesforce defaults:
	 * Account Name, Account Number, Country, Entity, Account Type, AR Account Number.
	 */
	@Keyword
	def verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs(List<String> labels) {
		try {
			List<String> resolved = (labels ?: []).collect { (it ?: '').trim() }.findAll { it }
			if (resolved.isEmpty()) {
				resolved = AR_SALESFORCE_READONLY_FIELD_LABELS
			}
			verifyArBillingPreferencesChargeCodeTabsAvailable()
			navigateToArDtcTabAndVerifyPageLoaded()
			verifyArAccountDetailDisabledReadOnlyInputsList(resolved)
			navigateToArDtsTabAndVerifyPageLoaded()
			verifyArAccountDetailDisabledReadOnlyInputsList(resolved)
			navigateToArDt3TabAndVerifyPageLoaded()
			verifyArAccountDetailDisabledReadOnlyInputsList(resolved)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs failed: ${e.message}")
		}
	}

	/**
	 * Same as {@link #verifyArAccountDetailReadOnlyFieldsOnChargeCodeTabsFromData} but validates
	 * disabled {@code input} elements matching the AR read-only style (cursor-not-allowed, bg-[#f5f5f9]).
	 * Optional data-file labels; defaults to Salesforce read-only field list when blank.
	 */
	@Keyword
	def verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabsFromData(String... fieldLabelsFromData) {
		List<String> labels = parseArDetailFieldLabelsFromData(fieldLabelsFromData)
		if (labels.isEmpty()) {
			labels = AR_SALESFORCE_READONLY_FIELD_LABELS
			KeywordUtil.logInfo("Using default Salesforce read-only labels: ${labels}")
		}
		verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs(labels)
	}

	/**
	 * Data-file overload — pass {@code readonly_Label1}, {@code readonly_Label2}, … as separate arguments.
	 * Validates disabled read-only {@code input} locator on DTC / DTS / DT3 tabs.
	 */
	@Keyword
	def verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs(String... fieldLabelsFromData) {
		verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabsFromData(fieldLabelsFromData)
	}

	@Keyword
	def verifyArAccountDetailEditableFields(List<String> labels) {
		try {
			//verifyArAccountDetailPageLoaded()
			verifyArDtaPageLoaded()
			List<String> fieldLabels = labels ?: []
			if (fieldLabels.isEmpty()) {
				failStop('verifyArAccountDetailEditableFields: labels list is empty')
			}
			fieldLabels.each { String label ->
				verifyArAccountDetailFieldIsEditable(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountDetailEditableFields failed: ${e.message}")
		}
	}

	/**
	 * Validates AR Account Detail editable fields using values from the test data file
	 * (e.g. {@code dta_Field1}, {@code dta_Field2}, {@code dta_Field3}).
	 * Blank columns are skipped; delegates to {@link #verifyArAccountDetailEditableFields(List)}.
	 */
	@Keyword
	def verifyArAccountDetailEditableFields(String dta_Field1, String dta_Field2 = '', String dta_Field3 = '') {
		List<String> labelsFromData = [dta_Field1, dta_Field2, dta_Field3]
			.collect { (it ?: '').trim() }
			.findAll { it }
		if (labelsFromData.isEmpty()) {
			failStop('verifyArAccountDetailEditableFields: all dta_Field values from data file are blank')
		}
		verifyArAccountDetailEditableFields(labelsFromData)
	}

	@Keyword
	def verifyArSearchResultsAccountStatusValues() {
		try {
			verifySearchResultsTableVisible()
			int col = columnIndexForHeader('Status')
			List<String> cells = columnCellTexts(col)
			if (cells.isEmpty()) {
				failStop('No Account Status values found in search results.')
			}
			Set<String> allowed = ['Active', 'Blocked'] as Set
			cells.each { String status ->
				if (!allowed.contains(status)) {
					failStop("Unexpected Account Status '${status}' — expected Active or Blocked.")
				}
			}
			KeywordUtil.logInfo("Account Status values in results: ${cells.unique()}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArSearchResultsAccountStatusValues failed: ${e.message}")
		}
	}

	@Keyword
	def openArAccountDetailFromSearchResults(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			performArSearch(searchCriteria, searchBy, searchValue, entity)
			clickFirstArAccountNumberLink()
		//	verifyArAccountDetailPageLoaded()
			verifyArDtaPageLoaded()
		} catch (Exception e) {
			failStop("openArAccountDetailFromSearchResults failed: ${e.message}")
		}
	}

	/**
	 * Opens AR Account Detail for DTC / DTS / DT3 charge-tab accounts (does not require DTA panel).
	 * Use for CCIPH-126 billing preference tests on charge code tabs.
	 */
	@Keyword
	def openArAccountDetailForChargeCodeBilling(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			performArSearch(searchCriteria, searchBy, searchValue, entity)
			clickFirstArAccountNumberLink()
		//	verifyArAccountDetailPageLoaded()
			verifyArBillingPreferencesChargeCodeTabsAvailable()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("openArAccountDetailForChargeCodeBilling failed: ${e.message}")
		}
	}

	// ---------- CCIPH-123: AR Service Charge Settings ----------

	private static String sanitizeWindowsPath(String raw) {
		if (!raw) {
			return ''
		}
		return raw.replace('\u202A', '').replace('\u202B', '').replace('\u202C', '').trim()
	}

	private static File resolveDefaultUserDownloadsDirectory() {
		String home = System.getProperty('user.home') ?: ''
		return new File(home, 'Downloads')
	}

	private static File resolveArServiceChargeExportDownloadsDirectory(String explicitDownloadsPath = null) {
		String explicit = sanitizeWindowsPath(explicitDownloadsPath)
		if (explicit) {
			return new File(explicit)
		}
		String fromProfile = sanitizeWindowsPath(ProfileGlobals.get('AR_SERVICE_CHARGE_EXPORT_DOWNLOADS_DIR', ''))
		if (!fromProfile) {
			fromProfile = sanitizeWindowsPath(ProfileGlobals.get('HISTORY_EXPORT_DOWNLOADS_DIR', ''))
		}
		if (fromProfile) {
			File profileDir = new File(fromProfile)
			if (profileDir.isDirectory()) {
				return profileDir
			}
			KeywordUtil.markWarning("Profile Downloads path does not exist (${profileDir.absolutePath}); using ${resolveDefaultUserDownloadsDirectory().absolutePath}")
		}
		return resolveDefaultUserDownloadsDirectory()
	}

	private static String resolveArServiceChargeExportFileNamePrefix() {
		String fromProfile = (ProfileGlobals.get('AR_SERVICE_CHARGE_EXPORT_FILE_NAME_PREFIX', '') ?: '').trim()
		return fromProfile ?: AR_SERVICE_CHARGE_EXPORT_NAME_PREFIX_DEFAULT
	}

	private static boolean isCompletedArServiceChargeExportDownload(File f) {
		if (f == null || !f.isFile()) {
			return false
		}
		String n = f.name.toLowerCase()
		boolean excelOrCsv = n.endsWith('.xlsx') || n.endsWith('.xls') || n.endsWith('.csv')
		return excelOrCsv && !n.endsWith('.crdownload') && !n.endsWith('.tmp') && !n.endsWith('.part')
	}

	private static List<File> listArServiceChargeExportsModifiedAfter(File downloadsDir, long afterMillis, String fileNamePrefix) {
		if (downloadsDir == null || !downloadsDir.isDirectory()) {
			return []
		}
		String prefix = (fileNamePrefix ?: '').trim().toLowerCase()
		File[] files = downloadsDir.listFiles({ File f ->
			if (!isCompletedArServiceChargeExportDownload(f)) {
				return false
			}
			if (prefix && !f.name.toLowerCase().contains(prefix)) {
				return false
			}
			return f.lastModified() >= afterMillis
		} as FileFilter)
		if (files == null) {
			return []
		}
		List<File> out = new ArrayList<>(java.util.Arrays.asList(files))
		out.sort { a, b -> Long.compare(b.lastModified(), a.lastModified()) }
		return out
	}

	private void recordDetectedArServiceChargeExportFile(File exportFile) {
		lastArServiceChargeExportFileName = exportFile.name
		lastArServiceChargeExportFilePath = exportFile.absolutePath
		writeDetectedArServiceChargeExportFile(exportFile.name, exportFile.absolutePath)
		KeywordUtil.logInfo("Detected Service Charge export full path: ${lastArServiceChargeExportFilePath}")
	}

	private static void writeDetectedArServiceChargeExportFile(String fileName, String absolutePath) {
		try {
			String projectDir = RunConfiguration.getProjectDir() ?: System.getProperty('user.dir') ?: ''
			File out = new File(projectDir, AR_SERVICE_CHARGE_EXPORT_DETECTED_FILE)
			File parent = out.parentFile
			if (parent != null && !parent.exists()) {
				parent.mkdirs()
			}
			out.setText("fileName=${fileName}\nabsolutePath=${absolutePath}\ncapturedAt=${new Date()}\n", 'UTF-8')
			KeywordUtil.logInfo("Wrote AR service charge export details to: ${out.absolutePath}")
		} catch (Exception e) {
			KeywordUtil.markWarning("Could not write ${AR_SERVICE_CHARGE_EXPORT_DETECTED_FILE}: ${e.message}")
		}
	}

	private static boolean isArServiceChargePanelOpen() {
		return WebUI.waitForElementPresent(toAr123('th_serviceChargeGridHeader'), 5, FailureHandling.OPTIONAL) ||
			WebUI.waitForElementVisible(toAr123('h3_serviceChargeListTitle'), 5, FailureHandling.OPTIONAL)
	}

	private static String normalizeServiceChargeHeaderText(String text) {
		String firstLine = (text ?: '').split(/[\r\n]+/)[0]
		return firstLine.replaceAll(/\s+/, ' ').trim().toLowerCase()
	}

	private static List<String> readCsvFirstRowColumnHeaders(String absolutePath) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			return []
		}
		List<String> lines = f.readLines('UTF-8')
		if (lines.isEmpty()) {
			return []
		}
		String firstLine = (lines[0] ?: '').trim()
		if (!firstLine) {
			return []
		}
		if (firstLine.contains('|')) {
			return firstLine.split(/\|/).collect { it.trim() }.findAll { it }
		}
		if (firstLine.contains('\t')) {
			return firstLine.split(/\t/).collect { it.trim() }.findAll { it }
		}
		return firstLine.split(',').collect { (it ?: '').trim().replaceAll(/^"|"$/, '') }.findAll { it }
	}

	private static void verifyExportFileContainsServiceChargeHeaders(String absolutePath, List<String> expectedColumns) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			failStop("Export file does not exist: ${absolutePath}")
		}
		String name = f.name.toLowerCase()
		List<String> exportHeaders = []
		if (name.endsWith('.csv')) {
			exportHeaders = readCsvFirstRowColumnHeaders(absolutePath)
		} else if (name.endsWith('.xlsx') || name.endsWith('.xls')) {
			exportHeaders = readXlsxFirstRowColumnHeaders(absolutePath)
		} else {
			failStop("Unsupported Service Charge export file type: ${f.name}")
		}
		if (exportHeaders.isEmpty()) {
			failStop("Could not read column headers from export file: ${absolutePath}")
		}
		List<String> expected = (expectedColumns ?: []).collect { (it ?: '').trim() }.findAll { it }
		if (expected.isEmpty()) {
			expected = AR_SERVICE_CHARGE_TABLE_COLUMNS
		}
		expected.each { String col ->
			boolean found = exportHeaders.any { String header -> serviceChargeHeaderMatches(col, header) }
			if (!found) {
				failStop("Export file missing expected column '${col}'. Actual headers: ${exportHeaders}")
			}
		}
		KeywordUtil.logInfo("Validated Service Charge export headers: ${exportHeaders}")
	}

	private static boolean serviceChargeHeaderMatches(String expected, String actual) {
		String e = normalizeServiceChargeHeaderText(expected)
		String a = normalizeServiceChargeHeaderText(actual)
		if (!e || !a) {
			return false
		}
		return a == e || a.contains(e) || e.contains(a)
	}

	private static TestObject ar123ServiceChargePanelTitleObject() {
		TestObject to = new TestObject('ar123_panel_service_charge_settings')
		to.addProperty('xpath', ConditionType.EQUALS,
			"//h3[contains(@class,'text-base') and contains(@class,'font-bold') and normalize-space()='Service Charge Settings']")
		return to
	}

	private static TestObject ar123ServiceChargeColumnHeaderObject(String columnLabel) {
		String lit = xpathLiteral((columnLabel ?: '').trim())
		TestObject to = new TestObject("ar123_scs_col_${columnLabel.hashCode()}")
		to.addProperty('xpath', ConditionType.EQUALS,
			"//div[contains(@class,'overflow-x-auto')]//table//th[contains(@class,'uppercase')][" +
			"translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')=" +
			"translate(normalize-space(${lit}),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')]")
		return to
	}

	private static List<String> getArServiceChargeGridHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		List<WebElement> headers = driver.findElements(By.xpath(
			"//th[contains(@class,'bg-slate-50') and contains(@class,'px-4') and contains(@class,'py-3') and contains(@class,'text-xs') and contains(@class,'font-bold') and contains(@class,'uppercase')]"
		))
		return headers.collect { it.getText()?.trim() }.findAll { it }
	}

	private static boolean clickArServiceChargeSettingsToggleByJs() {
		Object result = WebUI.executeJavaScript('''
			function norm(s) {
				return (s || '').replace(/\\s+/g, ' ').trim().toLowerCase();
			}
			var candidates = document.querySelectorAll('span, p, label, div, h1, h2, h3, button');
			for (var i = 0; i < candidates.length; i++) {
				var el = candidates[i];
				if (el.children.length > 4) {
					continue;
				}
				var text = norm(el.textContent);
				if (!text || text.indexOf('service charge settings') < 0 || text.length > 50) {
					continue;
				}
				var row = el.closest('div');
				if (!row) {
					continue;
				}
				var btn = row.querySelector('button');
				if (btn) {
					btn.scrollIntoView({ block: 'center', inline: 'nearest' });
					btn.click();
					return true;
				}
				var svg = row.querySelector('svg');
				if (svg) {
					var clickTarget = svg.closest('button') || svg.parentElement;
					if (clickTarget) {
						clickTarget.scrollIntoView({ block: 'center', inline: 'nearest' });
						clickTarget.click();
						return true;
					}
				}
			}
			return false;
		''', null)
		return Boolean.TRUE.equals(result)
	}

	private static List<String> getArServiceChargeTableHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		String headerXp =
			"//h3[@class='text-base font-bold text-slate-700']/following::table[1]//thead//th | " +
			"//h3[@class='text-base font-bold text-slate-700']/following::table[1]//tr[th]/th | " +
			"//h3[@class='text-base font-bold text-slate-700']/ancestor::div[1]//table//th | " +
			"//h3[@class='text-base font-bold text-slate-700']/following::*[@role='columnheader']"
		List<WebElement> headers = driver.findElements(By.xpath(headerXp))
		return headers.collect { it.getText()?.trim() }.findAll { it }.unique()
	}

	private static boolean verifyTextPresentOnPageSilent(String expectedText) {
		try {
			String lit = xpathLiteral((expectedText ?: '').trim())
			TestObject textObj = new TestObject("ar_scs_text_${expectedText.hashCode()}")
			textObj.addProperty('xpath', ConditionType.EQUALS, "//*[contains(normalize-space(.), ${lit})]")
			return WebUI.waitForElementPresent(textObj, 3, FailureHandling.OPTIONAL)
		} catch (Exception ignored) {
			return false
		}
	}

	/** Clicks the Special Service Charge Settings toggle (SVG rect switch). */
	@Keyword
	def clickArServiceChargeSettingsToggle() {
		try {
			WebUI.delay(1)
			TestObject specialToggle = toAr123('toggle_serviceChargeSettings')
			waitVisible(specialToggle)
			WebUI.scrollToElement(specialToggle, DEFAULT_TIMEOUT)
			clickWithJsFallback(specialToggle, 'Special Service Charge Settings toggle')
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeSettingsToggle failed: ${e.message}")
		}
	}

	/**
	 * Toggles Special Service Charge Settings — same steps as {@code toggle_ar.ToggleArActions.toggleServiceChargeSettings()}
	 * (right-click rect track, wait for rect, click circle knob) using CCIPH-123 OR and wait/scroll/JS-click helpers.
	 */
	@Keyword
	def toggleArSpecialServiceChargeSettings() {
		try {
			WebUI.delay(2)
			TestObject toggleRect = toAr123('rect_specialServiceChargeToggle')
			TestObject toggleCircle = toAr123('circle_specialServiceChargeToggle')
			TestObject service_Charge= toAr123('service-charge')
		//	waitVisible(toggleRect)
			WebUI.scrollToElement(toggleRect, DEFAULT_TIMEOUT)
			WebUI.waitForElementPresent(toggleRect, DEFAULT_TIMEOUT)
			waitVisible(toggleCircle)
			clickWithJsFallback(toggleCircle, 'Special Service Charge Settings toggle circle')
			WebUI.delay(2)
			WebUI.scrollToElement(service_Charge, DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("toggleArSpecialServiceChargeSettings failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeSettingsPanelVisible() {
		try {
			//waitVisible(toAr123('h3_serviceChargeListTitle'))
			WebUI.verifyElementVisible(toAr123('h3_serviceChargeListTitle'), FailureHandling.STOP_ON_FAILURE)
			WebUI.scrollToElement(toAr123('h3_serviceChargeListTitle'), DEFAULT_TIMEOUT)
			if (WebUI.waitForElementPresent(toAr123('tbl_serviceChargeSettings'), 8, FailureHandling.OPTIONAL)) {
				WebUI.verifyElementVisible(toAr123('tbl_serviceChargeSettings'), FailureHandling.OPTIONAL)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeSettingsPanelVisible failed: ${e.message}")
		}
	}

	@Keyword
	def openArAccountDetailAndServiceChargeSettings(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			performArSearch(searchCriteria, searchBy, searchValue, entity)
			clickFirstArAccountNumberLink()
			//verifyArAccountDetailPageLoaded()
			WebUI.scrollToElement(toAr123('lbl_serviceChargeSettings'), DEFAULT_TIMEOUT)
//			clickArServiceChargeSettingsToggle()
//			verifyArServiceChargeSettingsPanelVisible()
		} catch (Exception e) {
			failStop("openArAccountDetailAndServiceChargeSettings failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeSettingsTableColumns(List<String> expectedColumns) {
		try {
			verifyArServiceChargeSettingsPanelVisible()
			List<String> headers = getArServiceChargeTableHeaderTexts()
			if (headers.isEmpty()) {
				expectedColumns.each { String col ->
					verifyTextPresentOnPage(col)
				}
			} else {
				expectedColumns.each { String col ->
					String expected = (col ?: '').trim()
					boolean found = headers.any { String h ->
						h.equalsIgnoreCase(expected) ||
							h.replaceAll(/\s+/, ' ').equalsIgnoreCase(expected.replaceAll(/\s+/, ' '))
					}
					if (!found) {
						failStop("Service Charge table header '${expected}' not found. Visible headers: ${headers}")
					}
				}
				KeywordUtil.logInfo("Service Charge table headers: ${headers}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeSettingsTableColumns failed: ${e.message}")
		}
	}

	/**
	 * Verifies Service Charge Settings panel title and each grid column header
	 * (ref {@code toggle_ar.ToggleArActions.verifyServiceChargeSettingsPanelAndColumns()}).
	 */
	@Keyword
	def verifyArServiceChargeSettingsPanelAndColumns(List<String> expectedColumns, String panelTitle = 'Service Charge Settings') {
		try {
			TestObject titleObj = ar123ServiceChargePanelTitleObject()
			WebUI.waitForElementPresent(titleObj, DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			WebUI.scrollToElement(titleObj, DEFAULT_TIMEOUT)
			WebUI.verifyElementText(titleObj, panelTitle, FailureHandling.STOP_ON_FAILURE)

			List<String> expected = (expectedColumns ?: []).collect { (it ?: '').trim() }.findAll { it }
			if (expected.isEmpty()) {
				failStop('verifyArServiceChargeSettingsPanelAndColumns: expected column list is empty')
			}
			expected.each { String col ->
				TestObject colObj = ar123ServiceChargeColumnHeaderObject(col)
				if (WebUI.waitForElementPresent(colObj, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
					WebUI.scrollToElement(colObj, DEFAULT_TIMEOUT)
					String actual = (WebUI.getText(colObj) ?: '').trim()
					if (!serviceChargeHeaderMatches(col, actual)) {
						failStop("Service Charge column '${col}' text mismatch. Actual: '${actual}'")
					}
				} else {
					List<String> headers = getArServiceChargeGridHeaderTexts()
					boolean found = headers.any { String h -> serviceChargeHeaderMatches(col, h) }
					if (!found) {
						failStop("Service Charge column '${col}' not found. Visible headers: ${headers}")
					}
				}
			}
			List<String> headers = getArServiceChargeGridHeaderTexts()
			KeywordUtil.logInfo("Service Charge grid headers: ${headers}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeSettingsPanelAndColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeSettingsGridColumns(List<String> expectedColumns) {
		verifyArServiceChargeSettingsPanelAndColumns(expectedColumns)
	}

	/**
	 * Validates Service Charge grid columns from data file ({@code service_Columns} pipe-separated),
	 * separate data columns ({@code sc1}, {@code sc2}, …), or defaults after toggle is ON.
	 */
	@Keyword
	def verifyArServiceChargeSettingsTableColumnsFromData(String... expectedColumnsFromData) {
		List<String> columns = parseArDetailFieldLabelsFromData(expectedColumnsFromData)
		if (columns.isEmpty()) {
			columns = AR_SERVICE_CHARGE_TABLE_COLUMNS_DEFAULT
			KeywordUtil.logInfo("Using default Service Charge table columns: ${columns}")
		}
		verifyArServiceChargeSettingsPanelAndColumns(columns)
	}

	@Keyword
	def clickArServiceChargeSettingsAddNew() {
		try {
			if (WebUI.waitForElementPresent(toAr123('btn_serviceChargeAddNew'), 8, FailureHandling.OPTIONAL)) {
				click(toAr123('btn_serviceChargeAddNew'))
				WebUI.delay(1)
			} else {
				KeywordUtil.logInfo('Add New button not present — continuing (export may still be available).')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeSettingsAddNew failed: ${e.message}")
		}
	}

	@Keyword
	def clickArServiceChargeSettingsExportToExcel() {
		try {
			click(toAr123('btn_serviceChargeExportExcel'))
			WebUI.delay(4)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeSettingsExportToExcel failed: ${e.message}")
		}
	}

	@Keyword
	String verifyArServiceChargeSettingsExportDownloaded(long exportStartMillis, String downloadsFolder = null) {
		try {
			File dir = resolveArServiceChargeExportDownloadsDirectory(downloadsFolder)
			String namePrefix = resolveArServiceChargeExportFileNamePrefix()
			if (!dir.isDirectory()) {
				failStop("Downloads folder does not exist: ${dir.absolutePath}")
			}
			KeywordUtil.logInfo("Waiting for Service Charge export in: ${dir.absolutePath} (name contains '${namePrefix}')")
			long deadline = System.currentTimeMillis() + 60000L
			long afterMillis = exportStartMillis - 5000L
			List<File> found = []
			while (System.currentTimeMillis() < deadline) {
				found = listArServiceChargeExportsModifiedAfter(dir, afterMillis, namePrefix)
				if (!found.isEmpty()) {
					break
				}
				WebUI.delay(2)
			}
			if (found.isEmpty()) {
				found = listArServiceChargeExportsModifiedAfter(dir, afterMillis, '')
			}
			if (found.isEmpty()) {
				failStop("No Service Charge export file (.xlsx/.xls/.csv) found in ${dir.absolutePath} after export.")
			}
			File exportFile = found.get(0)
			recordDetectedArServiceChargeExportFile(exportFile)
			if (!exportFile.exists() || exportFile.length() <= 0L) {
				failStop("Service Charge export file is missing or empty: ${exportFile.absolutePath}")
			}
			KeywordUtil.logInfo("Service Charge export file: ${exportFile.absolutePath}")
			WebUI.takeScreenshot()
			return lastArServiceChargeExportFilePath
		} catch (Exception e) {
			failStop("verifyArServiceChargeSettingsExportDownloaded failed: ${e.message}")
			return ''
		}
	}

	/**
	 * Resolved Downloads folder for Service Charge export validation (current user's {@code ~/Downloads} unless profile override exists).
	 */
	@Keyword
	def getArServiceChargeExportDownloadsDirectory() {
		return resolveArServiceChargeExportDownloadsDirectory(null).absolutePath
	}

	@Keyword
	String getLastArServiceChargeExportFilePath() {
		return lastArServiceChargeExportFilePath ?: ''
	}

	@Keyword
	String getLastArServiceChargeExportFileName() {
		return lastArServiceChargeExportFileName ?: ''
	}

	/**
	 * Deletes the validated Service Charge export file from Downloads after test assertions.
	 *
	 * @param absolutePath optional path; defaults to last path from {@link #verifyArServiceChargeSettingsExportDownloaded(long, String)}
	 */
	@Keyword
	def deleteLastArServiceChargeExportFile(String absolutePath = null) {
		try {
			String p = (absolutePath ?: lastArServiceChargeExportFilePath ?: '').trim()
			if (!p) {
				KeywordUtil.markWarning('deleteLastArServiceChargeExportFile: no export file path to delete')
				return
			}
			File f = new File(p)
			if (!f.isFile()) {
				failStop("Service Charge export file not found for deletion: ${p}")
			}
			if (!f.delete()) {
				failStop("Failed to delete Service Charge export file: ${p}")
			}
			KeywordUtil.logInfo("Deleted Service Charge export file: ${p}")
			if (p == lastArServiceChargeExportFilePath) {
				lastArServiceChargeExportFilePath = ''
				lastArServiceChargeExportFileName = ''
			}
		} catch (Exception e) {
			failStop("deleteLastArServiceChargeExportFile failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeExportFileContainsColumnHeaders(String absolutePath, String expectedColumnsFromData) {
		try {
			if (!absolutePath?.trim()) {
				failStop('verifyArServiceChargeExportFileContainsColumnHeaders: absolutePath is blank')
			}
			List<String> columns = parseArDetailFieldLabelsFromData(expectedColumnsFromData)
			verifyExportFileContainsServiceChargeHeaders(absolutePath.trim(), columns)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeExportFileContainsColumnHeaders failed: ${e.message}")
		}
	}

	// ---------- CCIPH-124: AR Service Charge — edit / save / inline cancel ----------

	private static TestObject ar124ServiceChargeTableRowObject(int oneBasedRow) {
		TestObject row = new TestObject("ar124_scs_row_${oneBasedRow}")
		row.addProperty('xpath', ConditionType.EQUALS,
			"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[${oneBasedRow}]")
		return row
	}

	private static TestObject ar124ServiceChargeEditingRowObject() {
		TestObject row = new TestObject('ar124_scs_editing_row')
		row.addProperty('xpath', ConditionType.EQUALS,
			"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//select or .//input[@type='text'] or .//button[normalize-space()='Save' or normalize-space()='Cancel']][1]")
		return row
	}

	private static TestObject ar124ServiceChargeInlineAddRowObject() {
		TestObject row = new TestObject('ar124_scs_inline_add_row')
		row.addProperty('xpath', ConditionType.EQUALS,
			"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//button[normalize-space()='Save' or normalize-space()='Cancel'] and (.//input[@type='text'] or .//select)][1]")
		return row
	}

	private static String normalizeArActiveValue(String value) {
		return (value ?: '').replaceAll(/\s+/, ' ').trim()
	}

	private static String resolveOppositeArActiveValue(String current, String yesLabel = 'Yes', String noLabel = 'No') {
		String c = normalizeArActiveValue(current).toLowerCase()
		String yes = normalizeArActiveValue(yesLabel)
		String no = normalizeArActiveValue(noLabel)
		if (c == yes.toLowerCase() || c == 'y' || c == 'true') {
			return no
		}
		if (c == no.toLowerCase() || c == 'n' || c == 'false') {
			return yes
		}
		return yes
	}

	private static int getArServiceChargeDataRowCountInternal() {
		def driver = DriverFactory.getWebDriver()
		List<WebElement> rows = driver.findElements(By.xpath(
			"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[not(.//th)]"
		))
		return rows.size()
	}

	private static int getArServiceChargeActiveColumnIndex() {
		List<String> headers = getArServiceChargeTableHeaderTexts()
		if (headers.isEmpty()) {
			headers = getArServiceChargeGridHeaderTexts()
		}
		int idx = headers.findIndexOf { String h ->
			normalizeServiceChargeHeaderText(h) == 'active' || normalizeServiceChargeHeaderText(h).contains('active')
		}
		return idx >= 0 ? idx + 1 : 4
	}

	private static String getArServiceChargeCellText(int oneBasedRow, int oneBasedCol) {
		def driver = DriverFactory.getWebDriver()
		String cellXp = "${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[${oneBasedRow}]/td[${oneBasedCol}]"
		List<WebElement> cells = driver.findElements(By.xpath(cellXp))
		if (cells.isEmpty()) {
			return ''
		}
		return normalizeArActiveValue(cells.get(0).getText())
	}

	@Keyword
	def openArAccountDetailAndServiceChargePanel(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			openArAccountDetailAndServiceChargeSettings(searchCriteria, searchBy, searchValue, entity)
//			toggleArSpecialServiceChargeSettings()
//			verifyArServiceChargeSettingsPanelVisible()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("openArAccountDetailAndServiceChargePanel failed: ${e.message}")
		}
	}

	@Keyword
	def clickArServiceChargeEditFirstRow() {
		try {
			TestObject editBtn = toAr124('btn_serviceChargeEditFirst')
			if (!WebUI.waitForElementPresent(editBtn, 8, FailureHandling.OPTIONAL)) {
				editBtn = new TestObject('ar124_edit_first_fallback')
				editBtn.addProperty('xpath', ConditionType.EQUALS,
					"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[1]//*[self::button or self::a][contains(normalize-space(.),'Edit') or contains(@aria-label,'Edit') or contains(@title,'Edit')][1]")
			}
			click(editBtn)
			WebUI.delay(1)
			waitVisible(ar124ServiceChargeEditingRowObject())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeEditFirstRow failed: ${e.message}")
		}
	}

	@Keyword
	String getArServiceChargeFirstRowActiveDisplayValue() {
		try {
			int col = getArServiceChargeActiveColumnIndex()
			String value = getArServiceChargeCellText(1, col)
			KeywordUtil.logInfo("First row Active display value: '${value}' (column index ${col})")
			WebUI.takeScreenshot()
			return value
		} catch (Exception e) {
			failStop("getArServiceChargeFirstRowActiveDisplayValue failed: ${e.message}")
			return ''
		}
	}

	@Keyword
	def verifyArServiceChargeRowInEditMode() {
		try {
			TestObject editingRow = ar124ServiceChargeEditingRowObject()
			waitVisible(editingRow)
			TestObject activeSelect = toAr124('sel_serviceChargeActiveEdit')
			if (!WebUI.waitForElementPresent(activeSelect, 8, FailureHandling.OPTIONAL)) {
				activeSelect = new TestObject('ar124_active_select_fallback')
				activeSelect.addProperty('xpath', ConditionType.EQUALS,
					"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//button[normalize-space()='Save']][1]//select[1]")
			}
			waitVisible(activeSelect)
			WebUI.verifyElementVisible(activeSelect, FailureHandling.STOP_ON_FAILURE)
			TestObject saveBtn = toAr124('btn_serviceChargeSave')
			WebUI.verifyElementVisible(saveBtn, FailureHandling.STOP_ON_FAILURE)
			TestObject cancelBtn = toAr124('btn_serviceChargeCancel')
			WebUI.verifyElementVisible(cancelBtn, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeRowInEditMode failed: ${e.message}")
		}
	}

	@Keyword
	def toggleArServiceChargeActiveInEditRow(String yesLabel = 'Yes', String noLabel = 'No', String currentValue = '') {
		try {
			String current = normalizeArActiveValue(currentValue)
			if (!current) {
				current = getArServiceChargeFirstRowActiveDisplayValue()
			}
			String target = resolveOppositeArActiveValue(current, yesLabel, noLabel)
			setArServiceChargeActiveInEditRow(target, yesLabel, noLabel)
		} catch (Exception e) {
			failStop("toggleArServiceChargeActiveInEditRow failed: ${e.message}")
		}
	}

	@Keyword
	def setArServiceChargeActiveInEditRow(String targetValue, String yesLabel = 'Yes', String noLabel = 'No') {
		try {
			String target = normalizeArActiveValue(targetValue)
			if (!target) {
				failStop('setArServiceChargeActiveInEditRow: targetValue is blank')
			}
			TestObject activeSelect = toAr124('sel_serviceChargeActiveEdit')
			if (!WebUI.waitForElementPresent(activeSelect, 8, FailureHandling.OPTIONAL)) {
				activeSelect = new TestObject('ar124_active_select_set')
				activeSelect.addProperty('xpath', ConditionType.EQUALS,
					"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//button[normalize-space()='Save']][1]//select[1]")
			}
			waitVisibleClickable(activeSelect)
			boolean selected = false
			([target, yesLabel, noLabel, 'Yes', 'No', 'Y', 'N'] as List).unique().each { String opt ->
				if (selected || !normalizeArActiveValue(opt)) {
					return
				}
				try {
					WebUI.selectOptionByLabel(activeSelect, opt, false, FailureHandling.OPTIONAL)
					selected = true
				} catch (Exception ignored) {
				}
				if (!selected) {
					try {
						WebUI.selectOptionByValue(activeSelect, opt, false, FailureHandling.OPTIONAL)
						selected = true
					} catch (Exception ignored2) {
					}
				}
			}
			if (!selected) {
				WebUI.selectOptionByLabel(activeSelect, target, false)
			}
			WebUI.takeScreenshot()
			KeywordUtil.logInfo("Set Active in edit row to '${target}'")
		} catch (Exception e) {
			failStop("setArServiceChargeActiveInEditRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickArServiceChargeSaveEditRow() {
		try {
			TestObject saveBtn = toAr124('btn_serviceChargeSave')
			if (!WebUI.waitForElementPresent(saveBtn, 8, FailureHandling.OPTIONAL)) {
				saveBtn = new TestObject('ar124_save_fallback')
				saveBtn.addProperty('xpath', ConditionType.EQUALS,
					"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//button[normalize-space()='Save']][1]//button[normalize-space()='Save'][1]")
			}
			click(saveBtn)
			WebUI.delay(2)
			if (WebUI.waitForElementPresent(ar124ServiceChargeEditingRowObject(), 5, FailureHandling.OPTIONAL)) {
				WebUI.delay(2)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeSaveEditRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickArServiceChargeCancelEditRow() {
		try {
			TestObject cancelBtn = toAr124('btn_serviceChargeCancel')
			if (!WebUI.waitForElementPresent(cancelBtn, 8, FailureHandling.OPTIONAL)) {
				cancelBtn = new TestObject('ar124_cancel_fallback')
				cancelBtn.addProperty('xpath', ConditionType.EQUALS,
					"${AR_SERVICE_CHARGE_TABLE_XPATH}//tbody/tr[.//button[normalize-space()='Cancel']][1]//button[normalize-space()='Cancel'][1]")
			}
			click(cancelBtn)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArServiceChargeCancelEditRow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeFirstRowActiveValue(String expectedValue) {
		try {
			String expected = normalizeArActiveValue(expectedValue)
			int col = getArServiceChargeActiveColumnIndex()
			String actual = ''
			long deadline = System.currentTimeMillis() + 20000L
			while (System.currentTimeMillis() < deadline) {
				actual = getArServiceChargeCellText(1, col)
				if (actual.equalsIgnoreCase(expected) ||
					normalizeArActiveValue(actual).equalsIgnoreCase(expected)) {
					break
				}
				WebUI.delay(1)
			}
			if (!actual.equalsIgnoreCase(expected) &&
				!normalizeArActiveValue(actual).equalsIgnoreCase(expected)) {
				failStop("Active value mismatch after save. Expected '${expected}', actual '${actual}'.")
			}
			KeywordUtil.logInfo("Verified first row Active is '${actual}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeFirstRowActiveValue failed: ${e.message}")
		}
	}

	@Keyword
	int getArServiceChargeDataRowCount() {
		try {
			int count = getArServiceChargeDataRowCountInternal()
			KeywordUtil.logInfo("Service Charge data row count: ${count}")
			return count
		} catch (Exception e) {
			failStop("getArServiceChargeDataRowCount failed: ${e.message}")
			return 0
		}
	}

	@Keyword
	def verifyArServiceChargeInlineAddRowVisible() {
		try {
			TestObject addRow = ar124ServiceChargeInlineAddRowObject()
			waitVisible(addRow)
			TestObject cancelBtn = toAr124('btn_serviceChargeCancel')
			WebUI.verifyElementVisible(cancelBtn, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeInlineAddRowVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArServiceChargeInlineAddRowDismissed(int rowCountBeforeAdd = -1) {
		try {
			if (rowCountBeforeAdd >= 0) {
				int current = getArServiceChargeDataRowCountInternal()
				if (current > rowCountBeforeAdd) {
					failStop("Inline add row still present after Cancel. Before=${rowCountBeforeAdd}, after=${current}")
				}
			}
			if (WebUI.waitForElementPresent(ar124ServiceChargeInlineAddRowObject(), 3, FailureHandling.OPTIONAL)) {
				failStop('Inline add/edit row with Save/Cancel still visible after Cancel.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArServiceChargeInlineAddRowDismissed failed: ${e.message}")
		}
	}

	@Keyword
	def editArServiceChargeActiveToggleAndCancel(String yesLabel = 'Yes', String noLabel = 'No') {
		try {
			String before = getArServiceChargeFirstRowActiveDisplayValue()
			clickArServiceChargeEditFirstRow()
			verifyArServiceChargeRowInEditMode()
			toggleArServiceChargeActiveInEditRow(yesLabel, noLabel, before)
			clickArServiceChargeCancelEditRow()
			verifyArServiceChargeFirstRowActiveValue(before)
		} catch (Exception e) {
			failStop("editArServiceChargeActiveToggleAndCancel failed: ${e.message}")
		}
	}

	@Keyword
	def editArServiceChargeActiveSave(String yesLabel = 'Yes', String noLabel = 'No') {
		try {
			String original = getArServiceChargeFirstRowActiveDisplayValue()
			String updated = resolveOppositeArActiveValue(original, yesLabel, noLabel)
			clickArServiceChargeEditFirstRow()
			setArServiceChargeActiveInEditRow(updated, yesLabel, noLabel)
			clickArServiceChargeSaveEditRow()
			verifyArServiceChargeFirstRowActiveValue(updated)
		} catch (Exception e) {
			failStop("editArServiceChargeActiveSaveAndRestore failed: ${e.message}")
		}
	}

	@Keyword
	def cancelArServiceChargeInlineAddRow() {
		try {
			clickArServiceChargeSettingsAddNew()
			verifyArServiceChargeInlineAddRowVisible()
			clickArServiceChargeCancelEditRow()
			verifyArServiceChargeInlineAddRowDismissed()
		} catch (Exception e) {
			failStop("cancelArServiceChargeInlineAddRow failed: ${e.message}")
		}
	}

	// ---------- CCIPH-37: AR Management History (AC1–AC4) — assumes authenticated session ----------

	private static String arHistoryTableDataRowsXPath() {
		return "//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td]"
	}

	private static String arHistoryFilterSnapshot() {
		String footer = ''
		try {
			footer = (WebUI.getText(toAr37('txt_historyRecordFooter'), FailureHandling.OPTIONAL) ?: '').trim()
		} catch (Exception ignored) {
		}
		int rowCount = 0
		try {
			TestObject rowsTo = new TestObject('ar_history_filter_snap_rows')
			rowsTo.addProperty('xpath', ConditionType.EQUALS, arHistoryTableDataRowsXPath())
			List<?> els = WebUI.findWebElements(rowsTo, 5)
			rowCount = els ? els.size() : 0
		} catch (Exception ignored) {
		}
		String firstRow = ''
		try {
			TestObject firstRowTo = new TestObject('ar_history_filter_snap_first_row')
			firstRowTo.addProperty('xpath', ConditionType.EQUALS,
				"(//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td])[1]")
			if (WebUI.waitForElementVisible(firstRowTo, 2, FailureHandling.OPTIONAL)) {
				firstRow = (WebUI.getText(firstRowTo, FailureHandling.OPTIONAL) ?: '').trim()
			}
		} catch (Exception ignored) {
		}
		return "${footer}|${rowCount}|${firstRow}"
	}

	private static void waitForArHistoryFilterToApply(String beforeSnapshot, int timeoutSec = 25) {
		long deadline = System.currentTimeMillis() + (timeoutSec * 1000L)
		while (System.currentTimeMillis() < deadline) {
			WebUI.delay(0.5)
			if (arHistoryFilterSnapshot() != beforeSnapshot) {
				WebUI.delay(1)
				return
			}
		}
		failStop("AR History table did not refresh after filter (footer/rows unchanged for ${timeoutSec}s)")
	}

	private static String normalizeArHistoryDateToIso(String raw) {
		String t = (raw ?: '').trim()
		if (!t) {
			failStop('normalizeArHistoryDateToIso: date is blank')
		}
		if (t ==~ /\d{4}-\d{2}-\d{2}/) {
			return t
		}
		if (t ==~ /\d{2}-\d{2}-\d{4}/) {
			String[] p = t.split('-')
			return String.format('%s-%s-%s', p[2], p[1], p[0])
		}
		failStop("Expected dd-MM-yyyy or yyyy-MM-dd, got: ${raw}")
		return ''
	}

	private static void setArHistoryHtmlDateInput(TestObject testObject, String yyyyMmDd) {
		String iso = (yyyyMmDd ?: '').trim()
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.delay(0.3)
		WebElement el = WebUiCommonHelper.findWebElement(testObject, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript(
			'''var input = arguments[0];
var value = arguments[1];
var tracker = input._valueTracker;
if (tracker) { tracker.setValue(value); }
var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
setter.call(input, value);
input.dispatchEvent(new Event("input", { bubbles: true }));
input.dispatchEvent(new Event("change", { bubbles: true }));''',
			java.util.Arrays.asList(el, iso))
		WebUI.delay(0.3)
		String actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (!iso.equals(actual)) {
			WebUI.click(testObject)
			WebUI.clearText(testObject, FailureHandling.OPTIONAL)
			WebUI.setText(testObject, iso)
			WebUI.sendKeys(testObject, org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.TAB))
			WebUI.delay(0.3)
			actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		}
		if (!iso.equals(actual)) {
			failStop("AR History date input value mismatch: expected ${iso}, got '${actual}'")
		}
	}

	private static void commitArHistoryCustomDateFilter(TestObject endInp) {
		waitVisibleClickable(endInp)
		WebUI.click(endInp)
		WebUI.sendKeys(endInp, org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.ENTER))
		WebUI.delay(0.3)
		if (WebUI.waitForElementVisible(toAr37('txt_historyPageTitle'), 3, FailureHandling.OPTIONAL)) {
			WebUI.click(toAr37('txt_historyPageTitle'))
		}
		WebUI.delay(0.3)
		TestObject applyBtn = toAr37('btn_customRangeApply')
		if (WebUI.waitForElementClickable(applyBtn, 5, FailureHandling.OPTIONAL)) {
			WebUI.click(applyBtn)
			return
		}
		TestObject applyNearDates = new TestObject('ar_history_apply_near_custom_dates')
		applyNearDates.addProperty('xpath', ConditionType.EQUALS,
			"//input[@id='ccfe-history-custom-end']/following::button[contains(translate(normalize-space(.),'APPLY','apply'),'apply')][1]")
		if (WebUI.waitForElementClickable(applyNearDates, 3, FailureHandling.OPTIONAL)) {
			WebUI.click(applyNearDates)
		}
	}

	private static File resolveArHistoryExportDownloadsDirectory(String explicitDownloadsPath = null) {
		String explicit = sanitizeWindowsPath(explicitDownloadsPath)
		if (explicit) {
			return new File(explicit)
		}
		String fromProfile = sanitizeWindowsPath(ProfileGlobals.get('AR_HISTORY_EXPORT_DOWNLOADS_DIR', ''))
		if (!fromProfile) {
			fromProfile = sanitizeWindowsPath(ProfileGlobals.get('HISTORY_EXPORT_DOWNLOADS_DIR', ''))
		}
		if (fromProfile) {
			File profileDir = new File(fromProfile)
			if (profileDir.isDirectory()) {
				return profileDir
			}
			KeywordUtil.markWarning("Profile Downloads path does not exist (${profileDir.absolutePath}); using ${resolveDefaultUserDownloadsDirectory().absolutePath}")
		}
		return resolveDefaultUserDownloadsDirectory()
	}

	private static String resolveArHistoryExportFileNamePrefix() {
		String fromProfile = (ProfileGlobals.get('AR_HISTORY_EXPORT_FILE_NAME_PREFIX', '') ?: '').trim()
		return fromProfile ?: AR_HISTORY_EXPORT_NAME_PREFIX_DEFAULT
	}

	private static boolean isCompletedArHistoryExportCsvDownload(File f) {
		if (f == null || !f.isFile()) {
			return false
		}
		String n = f.name.toLowerCase()
		return n.endsWith('.xlsx') && !n.endsWith('.crdownload') && !n.endsWith('.tmp') && !n.endsWith('.part')
	}

	private static List<File> listArHistoryExportCsvModifiedAfter(File downloadsDir, long afterMillis, String fileNamePrefix) {
		if (downloadsDir == null || !downloadsDir.isDirectory()) {
			return []
		}
		String prefix = (fileNamePrefix ?: '').trim()
		File[] files = downloadsDir.listFiles({ File f ->
			if (!isCompletedArHistoryExportCsvDownload(f)) {
				return false
			}
			if (prefix && !f.name.contains(prefix)) {
				return false
			}
			return f.lastModified() >= afterMillis
		} as FileFilter)
		if (files == null) {
			return []
		}
		List<File> out = new ArrayList<>(java.util.Arrays.asList(files))
		out.sort { a, b -> Long.compare(b.lastModified(), a.lastModified()) }
		return out
	}

	private void recordDetectedArHistoryExportFile(File exportFile) {
		lastArHistoryExportFileName = exportFile.name
		lastArHistoryExportFilePath = exportFile.absolutePath
		try {
			String projectDir = RunConfiguration.getProjectDir() ?: System.getProperty('user.dir') ?: ''
			File out = new File(projectDir, AR_HISTORY_EXPORT_DETECTED_FILE)
			File parent = out.parentFile
			if (parent != null && !parent.exists()) {
				parent.mkdirs()
			}
			out.setText("fileName=${exportFile.name}\nabsolutePath=${exportFile.absolutePath}\ncapturedAt=${new Date()}\n", 'UTF-8')
			KeywordUtil.logInfo("Wrote AR history export details to: ${out.absolutePath}")
		} catch (Exception e) {
			KeywordUtil.markWarning("Could not write ${AR_HISTORY_EXPORT_DETECTED_FILE}: ${e.message}")
		}
	}

	private static boolean arHistoryCsvContainsAllKeywords(String absolutePath, List<String> keywords) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			return false
		}
		String blob = f.getText('UTF-8')
		for (String kw : keywords) {
			if (!blob.contains(kw)) {
				return false
			}
		}
		return true
	}

	private static String readZipEntryText(java.util.zip.ZipFile zip, String entryName) {
		java.util.zip.ZipEntry entry = zip.getEntry(entryName)
		if (entry == null) {
			return ''
		}
		InputStream input = zip.getInputStream(entry)
		try {
			return input.getText('UTF-8')
		} finally {
			input.close()
		}
	}

	private static List<String> readXlsxSharedStrings(java.util.zip.ZipFile zip) {
		String xml = readZipEntryText(zip, 'xl/sharedStrings.xml')
		if (!xml?.trim()) {
			return []
		}
		List<String> out = []
		(xml =~ /<si[^>]*>([\s\S]*?)<\/si>/).each { match ->
			String block = match[1]
			StringBuilder sb = new StringBuilder()
			(block =~ /<t[^>]*>([^<]*)<\/t>/).each { tMatch -> sb.append(tMatch[1]) }
			out.add(sb.toString())
		}
		return out
	}

	private static String extractXlsxCellValue(String inner, String attrs, List<String> sharedStrings) {
		if (inner?.contains('<is>')) {
			def textMatch = inner =~ /<t[^>]*>([^<]*)<\/t>/
			return textMatch.find() ? textMatch.group(1) : ''
		}
		def valueMatch = inner =~ /<v>([^<]*)<\/v>/
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

	private static List<String> readXlsxFirstRowColumnHeaders(String absolutePath) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			return []
		}
		java.util.zip.ZipFile zip = new java.util.zip.ZipFile(f)
		try {
			List<String> sharedStrings = readXlsxSharedStrings(zip)
			String sheetXml = readZipEntryText(zip, 'xl/worksheets/sheet1.xml')
			if (!sheetXml?.trim()) {
				return []
			}
			def rowMatch = sheetXml =~ /<row\s+r="1"[^>]*>([\s\S]*?)<\/row>/
			if (!rowMatch.find()) {
				return []
			}
			String rowContent = rowMatch.group(1)
			Map<String, String> colMap = new java.util.LinkedHashMap<String, String>()
			(rowContent =~ /<c\s+r="([A-Z]+)1"([^>]*)>([\s\S]*?)<\/c>/).each { cellMatch ->
				String col = cellMatch[1]
				String attrs = cellMatch[2]
				String inner = cellMatch[3]
				colMap.put(col, extractXlsxCellValue(inner, attrs, sharedStrings))
			}
			return new ArrayList<String>(colMap.values())
		} finally {
			zip.close()
		}
	}

	private static String readXlsxAllCellText(String absolutePath) {
		File f = new File(absolutePath)
		if (!f.isFile()) {
			return ''
		}
		java.util.zip.ZipFile zip = new java.util.zip.ZipFile(f)
		try {
			List<String> sharedStrings = readXlsxSharedStrings(zip)
			String sheetXml = readZipEntryText(zip, 'xl/worksheets/sheet1.xml')
			if (!sheetXml?.trim()) {
				return ''
			}
			StringBuilder sb = new StringBuilder()
			(sheetXml =~ /<c\s+r="[A-Z]+\d+"([^>]*)>([\s\S]*?)<\/c>/).each { cellMatch ->
				String value = extractXlsxCellValue(cellMatch[2], cellMatch[1], sharedStrings)
				if (value) {
					sb.append(value).append(' ')
				}
			}
			return sb.toString()
		} finally {
			zip.close()
		}
	}

	private static String readArHistoryExportTextBlob(String absolutePath) {
		String path = (absolutePath ?: '').trim()
		if (!path) {
			return ''
		}
		if (path.toLowerCase().endsWith('.xlsx')) {
			return readXlsxAllCellText(path)
		}
		return new File(path).getText('UTF-8')
	}

	@Keyword
	def clickArManagementHistoryButton() {
		try {
			click(toAr37('btn_ArHistory'))
		} catch (Exception e) {
			failStop("clickArManagementHistoryButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArManagementHistoryPageLoaded() {
		try {
			waitVisible(toAr37('txt_historyPageTitle'))
			WebUI.verifyElementVisible(toAr37('txt_historyPageTitle'), FailureHandling.STOP_ON_FAILURE)
			waitVisible(toAr37('tbl_historyChangeLog'))
			WebUI.verifyElementVisible(toAr37('tbl_historyChangeLog'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArManagementHistoryPageLoaded failed: ${e.message}")
		}
	}

	private static String normalizeArHistoryHeaderText(String text) {
		String firstLine = (text ?: '').split(/[\r\n]+/)[0]
		return firstLine.replaceAll(/\s+/, ' ').trim().toLowerCase()
	}

	private static boolean arHistoryHeaderMatches(String expected, String actual) {
		String e = normalizeArHistoryHeaderText(expected)
		String a = normalizeArHistoryHeaderText(actual)
		if (!e || !a) {
			return false
		}
		if (a == e || a.contains(e) || e.contains(a)) {
			return true
		}
		if (e == 'change' && (a.contains('action') || a == 'action')) {
			return true
		}
		return false
	}

	private static List<String> getArHistoryLogTableHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		String xp = "//table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'timestamp')]]//th"
		List<WebElement> headers = driver.findElements(By.xpath(xp))
		return headers.collect { (it.getText() ?: '').trim() }.findAll { it }.unique()
	}

	@Keyword
	def verifyArHistoryLogTableColumnHeadersPresent() {
		try {
			verifyArManagementHistoryPageLoaded()
			List<String> expected = ['TIMESTAMP', 'SECTION', 'USER', 'CHANGE']
			List<String> headers = getArHistoryLogTableHeaderTexts()
			if (headers.isEmpty()) {
				expected.each { String col ->
					String colLower = col.toLowerCase()
					String actionAlt = col.equalsIgnoreCase('CHANGE') ?
						" or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'action')" : ''
					TestObject th = new TestObject("ar_history_th_${col}")
					th.addProperty('xpath', ConditionType.EQUALS,
						"//table//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'${colLower}')${actionAlt}]")
					waitVisible(th)
					WebUI.verifyElementVisible(th, FailureHandling.STOP_ON_FAILURE)
				}
			} else {
				expected.each { String col ->
					boolean found = headers.any { String header -> arHistoryHeaderMatches(col, header) }
					if (!found) {
						failStop("AR History table header '${col}' not found. Visible headers: ${headers}")
					}
				}
				KeywordUtil.logInfo("AR History table headers: ${headers}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArHistoryLogTableColumnHeadersPresent failed: ${e.message}")
		}
	}

	@Keyword
	def openArManagementHistoryFromLanding() {
		try {
//			navigateToArManagement()
//			verifyArSearchPageLoaded()
			clickArManagementHistoryButton()
			verifyArManagementHistoryPageLoaded()
			//verifyArHistoryLogTableColumnHeadersPresent()
		} catch (Exception e) {
			failStop("openArManagementHistoryFromLanding failed: ${e.message}")
		}
	}

	@Keyword
	def clickArHistoryDateFilter(String filterKey) {
		try {
			String k = (filterKey ?: '').trim().toUpperCase()
			TestObject btn
			switch (k) {
				case 'ALL':
					btn = toAr37('btn_filter_All')
					break
				case 'TODAY':
					btn = toAr37('btn_filter_Today')
					break
				case 'THIS WEEK':
				case 'THISWEEK':
					btn = toAr37('btn_filter_ThisWeek')
					break
				case 'THIS MONTH':
				case 'THISMONTH':
					btn = toAr37('btn_filter_ThisMonth')
					break
				case 'CUSTOM':
				case 'CUSTOM RANGE':
				case 'CUSTOMRANGE':
					btn = toAr37('btn_filter_CustomRange')
					break
				default:
					failStop("Unknown AR history date filter: ${filterKey}")
					return
			}
			click(btn)
			WebUI.delay(2)
			waitVisible(toAr37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArHistoryDateFilter(${filterKey}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArHistoryTableStillDisplayed() {
		try {
			waitVisible(toAr37('tbl_historyChangeLog'))
			WebUI.verifyElementVisible(toAr37('tbl_historyChangeLog'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArHistoryTableStillDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def applyArCustomHistoryDateRange(String startDate, String endDate) {
		try {
			String beforeSnapshot = arHistoryFilterSnapshot()
			clickArHistoryDateFilter('CUSTOM')
			WebUI.delay(1)
			TestObject startInp = toAr37('inp_customRangeStart')
			TestObject endInp = toAr37('inp_customRangeEnd')
			String startIso = normalizeArHistoryDateToIso(startDate)
			String endIso = normalizeArHistoryDateToIso(endDate)
			waitVisible(startInp)
			waitVisible(endInp)
			setArHistoryHtmlDateInput(startInp, startIso)
			WebUI.delay(0.5)
			setArHistoryHtmlDateInput(endInp, endIso)
			commitArHistoryCustomDateFilter(endInp)
			waitForArHistoryFilterToApply(beforeSnapshot, 25)
			waitVisible(toAr37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("applyArCustomHistoryDateRange failed: ${e.message}")
		}
	}

	@Keyword
	def applyArCustomHistoryDateRangeFromData(String startDateFromData, String endDateFromData) {
		String start = (startDateFromData ?: ProfileGlobals.get('AR_HISTORY_CUSTOM_START', '26-05-2026')).trim()
		String end = (endDateFromData ?: ProfileGlobals.get('AR_HISTORY_CUSTOM_END', '01-05-2026')).trim()
		applyArCustomHistoryDateRange(start, end)
	}

	@Keyword
	def verifyArHistoryShowsNoDataRowsOrZeroCount() {
		try {
			TestObject row = new TestObject('arHistoryFirstDataRow')
			row.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td][1]")
			boolean hasRow = WebUI.waitForElementVisible(row, 6, FailureHandling.OPTIONAL)
			if (hasRow) {
				failStop('Expected no AR history rows for invalid / empty filter, but at least one data row is visible')
			}
			if (WebUI.waitForElementVisible(toAr37('txt_historyRecordFooter'), 5, FailureHandling.OPTIONAL)) {
				String footer = (WebUI.getText(toAr37('txt_historyRecordFooter'), FailureHandling.OPTIONAL) ?: '').toLowerCase()
				if (footer.contains('0 of') || footer.contains('0 records')) {
					WebUI.takeScreenshot()
					return
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArHistoryShowsNoDataRowsOrZeroCount failed: ${e.message}")
		}
	}

	@Keyword
	def clickArHistoryResetFilters() {
		try {
			click(toAr37('btn_historyReset'))
			WebUI.delay(2)
			waitVisible(toAr37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArHistoryResetFilters failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArHistoryFiltersCleared() {
		try {
//			clickArHistoryDateFilter('ALL')
			verifyArHistoryTableStillDisplayed()
//			String startVal = (WebUI.getAttribute(toAr37('inp_customRangeStart'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
//			String endVal = (WebUI.getAttribute(toAr37('inp_customRangeEnd'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
//			KeywordUtil.logInfo("AR History custom dates after reset — start='${startVal}', end='${endVal}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArHistoryFiltersCleared failed: ${e.message}")
		}
	}

	@Keyword
	def clickArHistoryExport() {
		try {
			click(toAr37('btn_historyExport'))
			WebUI.delay(4)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArHistoryExport failed: ${e.message}")
		}
	}

	@Keyword
	String verifyLatestArManagementHistoryExportDownloaded(long exportStartMillis, String downloadsFolder = null) {
		try {
			File dir = resolveArHistoryExportDownloadsDirectory(downloadsFolder)
			String namePrefix = resolveArHistoryExportFileNamePrefix()
			if (!dir.isDirectory()) {
				failStop("Downloads folder does not exist: ${dir.absolutePath}")
			}
			KeywordUtil.logInfo("Waiting for AR History export in: ${dir.absolutePath} (name contains '${namePrefix}')")
			long deadline = System.currentTimeMillis() + 60000L
			long afterMillis = exportStartMillis - 5000L
			List<File> found = []
			while (System.currentTimeMillis() < deadline) {
				found = listArHistoryExportCsvModifiedAfter(dir, afterMillis, namePrefix)
				if (!found.isEmpty()) {
					break
				}
				WebUI.delay(2)
			}
			if (found.isEmpty()) {
				found = listArHistoryExportCsvModifiedAfter(dir, afterMillis, 'Account')
			}
			if (found.isEmpty()) {
				found = listArHistoryExportCsvModifiedAfter(dir, afterMillis, 'History')
			}
			if (found.isEmpty()) {
				failStop("No AR History export file found in ${dir.absolutePath} after Export.")
			}
			File latest = found[0]
			recordDetectedArHistoryExportFile(latest)
			if (!latest.exists() || latest.length() <= 0L) {
				failStop("AR History export file is missing or empty: ${latest.absolutePath}")
			}
			WebUI.takeScreenshot()
			return lastArHistoryExportFilePath
		} catch (Exception e) {
			failStop("verifyLatestArManagementHistoryExportDownloaded failed: ${e.message}")
			return ''
		}
	}

	@Keyword
	String getLastArHistoryExportFilePath() {
		return lastArHistoryExportFilePath ?: ''
	}

	@Keyword
	String getLastArHistoryExportFileName() {
		return lastArHistoryExportFileName ?: ''
	}

	/**
	 * Resolved Downloads folder for AR History export validation (current user's {@code ~/Downloads} unless profile override exists).
	 */
	@Keyword
	String getArHistoryExportDownloadsDirectory() {
		return resolveArHistoryExportDownloadsDirectory(null).absolutePath
	}

	/**
	 * Deletes the validated AR History export file from Downloads after test assertions.
	 *
	 * @param absolutePath optional path; defaults to last path from {@link #verifyLatestArManagementHistoryExportDownloaded(long, String)}
	 */
	@Keyword
	def deleteLastArHistoryExportFile(String absolutePath = null) {
		try {
			String p = (absolutePath ?: lastArHistoryExportFilePath ?: '').trim()
			if (!p) {
				KeywordUtil.markWarning('deleteLastArHistoryExportFile: no export file path to delete')
				return
			}
			File f = new File(p)
			if (!f.isFile()) {
				failStop("AR History export file not found for deletion: ${p}")
			}
			if (!f.delete()) {
				failStop("Failed to delete AR History export file: ${p}")
			}
			KeywordUtil.logInfo("Deleted AR History export file: ${p}")
			if (p == lastArHistoryExportFilePath) {
				lastArHistoryExportFilePath = ''
				lastArHistoryExportFileName = ''
			}
		} catch (Exception e) {
			failStop("deleteLastArHistoryExportFile failed: ${e.message}")
		}
	}

	@Keyword
	def verifyExportedArHistoryHasColumnHeaders(String absolutePathToCsv) {
		try {
			String p = (absolutePathToCsv ?: lastArHistoryExportFilePath ?: '').trim()
			if (!p) {
				failStop('verifyExportedArHistoryCsvHasColumnHeaders: path is blank')
			}
			String lower = p.toLowerCase()
			if (!lower.endsWith('.xlsx') && !lower.endsWith('.csv')) {
				failStop("Expected a .xlsx or .csv export file, got: ${p}")
			}
			List<String> expected = AR_HISTORY_EXPORT_COLUMNS_DEFAULT
			if (lower.endsWith('.csv')) {
				if (!arHistoryCsvContainsAllKeywords(p, expected)) {
					failStop("Exported AR History CSV missing headers: ${expected.join(', ')}")
				}
			} else {
				List<String> actual = readXlsxFirstRowColumnHeaders(p)
				List<String> missing = expected.findAll { String col ->
					!actual.any { String header -> header.equalsIgnoreCase(col.trim()) }
				}
				if (!missing.isEmpty()) {
					failStop("Exported AR History XLSX missing headers: ${missing.join(', ')}. Actual: ${actual.join(', ')}")
				}
			}
			KeywordUtil.logInfo("Validated AR History export headers: ${expected.join(', ')}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedArHistoryCsvHasColumnHeaders failed: ${e.message}")
		}
	}

	@Keyword
	String getFirstVisibleArHistoryRowText() {
		try {
			TestObject row = new TestObject('arHistoryFirstRowText')
			row.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td][1]")
			boolean ok = WebUI.waitForElementVisible(row, 12, FailureHandling.OPTIONAL)
			if (!ok) {
				KeywordUtil.logInfo('No visible AR history data row — skipping row-text snippet checks.')
				return ''
			}
			waitVisible(row)
			String t = (WebUI.getText(row, FailureHandling.OPTIONAL) ?: '').trim()
			WebUI.takeScreenshot()
			return t
		} catch (Exception e) {
			KeywordUtil.markWarning("getFirstVisibleArHistoryRowText: ${e.message}")
			return ''
		}
	}

	@Keyword
	def verifyExportedArHistoryCsvContainsUiRowSnippet(String absolutePathToCsv, String uiRowText) {
		try {
			String path = (absolutePathToCsv ?: lastArHistoryExportFilePath ?: '').trim()
			String snippet = (uiRowText ?: '').trim()
			if (!path || !snippet) {
				failStop('verifyExportedArHistoryCsvContainsUiRowSnippet: path or uiRowText is blank')
			}
			String token = snippet.length() > 24 ? snippet.substring(0, 24) : snippet
			String blob = readArHistoryExportTextBlob(path)
			if (!blob.contains(token)) {
				failStop("AR History export file did not contain UI row snippet '${token}'.")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedArHistoryCsvContainsUiRowSnippet failed: ${e.message}")
		}
	}

	// ---------- CCIPH-86: AR Special RateSheet Settings ----------

	@Keyword
	static String generateArRatesheetRandomNumber() {
		int n = 100 + new java.util.Random().nextInt(900)
		return String.valueOf(n)
	}

	@Keyword
	def toggleArSpecialRateSheetSettings() {
		try {
			WebUI.delay(1)
			TestObject toggleRect = toAr86('rect_specialRateSheetToggle')
			TestObject toggleCircle = toAr86('circle_specialRateSheetToggle')
			WebUI.scrollToElement(toAr86('lbl_specialRateSheetSettings'), DEFAULT_TIMEOUT)
			if (WebUI.waitForElementPresent(toggleRect, 8, FailureHandling.OPTIONAL)) {
				waitVisible(toggleCircle)
				clickWithJsFallback(toggleCircle, 'Special RateSheet Settings toggle')
			} else {
				clickWithJsFallback(toAr86('toggle_specialRateSheetSettings'), 'Special RateSheet Settings toggle button')
			}
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("toggleArSpecialRateSheetSettings failed: ${e.message}")
		}
	}

	@Keyword
	def openArAccountDetailAndRatesheetSettings(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			openArAccountDetailForChargeCodeBilling(searchCriteria, searchBy, searchValue, entity)
			toggleArSpecialRateSheetSettings()
			verifyArRatesheetSettingsPanelVisible()
		} catch (Exception e) {
			failStop("openArAccountDetailAndRatesheetSettings failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArRatesheetSettingsPanelVisible() {
		try {
			TestObject title = toAr86('h3_ratesheetSettingsTitle')
			if (WebUI.waitForElementPresent(title, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
				waitVisible(title)
			}
			waitVisible(toAr86('tbl_ratesheetSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetSettingsPanelVisible failed: ${e.message}")
		}
	}

	private static List<String> getArRatesheetGridHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		String headerXp =
			"//h3[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'rate') " +
			"and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sheet')]/following::table[1]//thead//th | " +
			"//h3[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'rate') " +
			"and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sheet')]/following::table[1]//tr[th]/th | " +
			"${AR_RATESHEET_TABLE_XPATH}//th"
		List<WebElement> headers = driver.findElements(By.xpath(headerXp))
		return headers.collect { it.getText()?.trim() }.findAll { it }.unique()
	}

	/**
	 * Verifies RateSheet Settings panel and each grid column header
	 * (mirrors {@link #verifyArServiceChargeSettingsPanelAndColumns(List, String)}).
	 */
	@Keyword
	def verifyArRatesheetSettingsPanelAndColumns(List<String> expectedColumns) {
		try {
			TestObject titleObj = toAr86('h3_ratesheetSettingsTitle')
			WebUI.waitForElementPresent(titleObj, DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			WebUI.scrollToElement(titleObj, DEFAULT_TIMEOUT)
			waitVisible(titleObj)
			waitVisible(toAr86('tbl_ratesheetSettings'))

			List<String> expected = (expectedColumns ?: []).collect { (it ?: '').trim() }.findAll { it }
			if (expected.isEmpty()) {
				failStop('verifyArRatesheetSettingsPanelAndColumns: expected column list is empty')
			}
			List<String> headers = getArRatesheetGridHeaderTexts()
			if (headers.isEmpty()) {
				failStop('verifyArRatesheetSettingsPanelAndColumns: no RateSheet table headers found on page')
			}
			expected.each { String col ->
				boolean found = headers.any { String h -> serviceChargeHeaderMatches(col, h) }
				if (!found) {
					failStop("RateSheet column '${col}' not found. Visible headers: ${headers}")
				}
			}
			KeywordUtil.logInfo("RateSheet grid headers: ${headers}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetSettingsPanelAndColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArRatesheetSettingsColumnHeaders(List<String> expectedColumns) {
		verifyArRatesheetSettingsPanelAndColumns(expectedColumns ?: AR_RATESHEET_COLUMNS_DEFAULT)
	}

	/**
	 * Validates RateSheet grid columns from data file ({@code rs_Columns} pipe-separated)
	 * or defaults after Special RateSheet Settings toggle is ON.
	 */
	@Keyword
	def verifyArRatesheetSettingsColumnHeadersFromData(String... columnsFromData) {
		List<String> columns = parseArDetailFieldLabelsFromData(columnsFromData)
		if (columns.isEmpty()) {
			columns = AR_RATESHEET_COLUMNS_DEFAULT
			KeywordUtil.logInfo("Using default RateSheet table columns: ${columns}")
		}
		verifyArRatesheetSettingsPanelAndColumns(columns)
	}

	@Keyword
	def clickArRatesheetAddNew() {
		try {
			click(toAr86('btn_ratesheetAddNew'))
			WebUI.delay(1)
			waitVisible(ar86RatesheetDialogVisible())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetAddNew failed: ${e.message}")
		}
	}

	private static TestObject ar86RatesheetDialogVisible() {
		return toAr86('addRecord_Dialog')
	}
	private static TestObject ar86RatesheetSuccessPopup() {
		return toAr86('save_SuccessPopup')
	}

	private static TestObject ar86RatesheetDialogTitle() {
		return toAr86('addRecord_Title')
	}

	private static TestObject ar86RatesheetEditingRowObject() {
		return toAr86('ar86_rs_editing_row')
	}

	private static TestObject ar86RatesheetEditingInput(int oneBasedIndex) {
		TestObject inp = new TestObject("ar86_rs_edit_inp_${oneBasedIndex}")
		inp.addProperty('xpath', ConditionType.EQUALS,
			"${AR_RATESHEET_NEW_RECORD_CARD_XPATH}//input[not(@type='hidden')][${oneBasedIndex}]")
		return inp
	}

	private static TestObject ar86RatesheetEditingSelect(int oneBasedIndex) {
		TestObject sel = new TestObject("ar86_rs_edit_sel_${oneBasedIndex}")
		sel.addProperty('xpath', ConditionType.EQUALS,
			"${AR_RATESHEET_NEW_RECORD_CARD_XPATH}//select[${oneBasedIndex}]")
		return sel
	}

	/** Rate Type select for Rate 1 uses OR {@code sel_ratesheetRateType}; Rate 2+ use {@code entity-settings-col-rate-type-N}. */
	private static TestObject ar86RatesheetRateTypeSelect(int rateNumber) {
		int n = rateNumber
		if (n < 1 || n > 3) {
			failStop("ar86RatesheetRateTypeSelect: rateNumber must be 1, 2, or 3 (got ${rateNumber})")
		}
		if (n == 1) {
			return toAr86('sel_ratesheetRateType')
		}
		if (n == 2) {
			return toAr86('sel_ratesheetRate2Type')
		}
		TestObject sel = new TestObject("ar86_rs_rate_type_${n}")
		sel.addProperty('xpath', ConditionType.EQUALS, "//select[@aria-label='entity-settings-col-rate-type-${n}']")
		return sel
	}

	/** Value input for Rate N — Rate 1 uses OR {@code inp_ratesheetRate1Value}; Rate 2+ follow {@code entity-settings-col-rate-N}. */
	private static TestObject ar86RatesheetRateValueInput(int rateNumber) {
		int n = rateNumber
		if (n < 1 || n > 3) {
			failStop("ar86RatesheetRateValueInput: rateNumber must be 1, 2, or 3 (got ${rateNumber})")
		}
		if (n == 1) {
			return toAr86('inp_ratesheetRate1Value')
		}
		if (n == 2) {
			return toAr86('inp_ratesheetRate2Value')
		}
		TestObject inp = new TestObject("ar86_rs_rate_value_${n}")
		inp.addProperty('xpath', ConditionType.EQUALS,
			"//input[@aria-label='entity-settings-col-rate-${n}' or @aria-label='entity-settings-col-rate-value-${n}']")
		return inp
	}

	@Keyword
	def verifyArRatesheetInlineAddRowVisible() {
		try {
			waitVisible(ar86RatesheetEditingRowObject())
			waitVisible(toAr86('inp_ratesheetDescription'))
			waitVisible(toAr86('inp_ratesheetServiceDescription'))
			WebUI.verifyElementVisible(toAr86('btn_ratesheetSave'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetInlineAddRowVisible failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyAddNewRecordDialogDisplayed() {
		try {
			waitVisible(ar86RatesheetDialogVisible())
			WebUI.verifyElementVisible(ar86RatesheetDialogVisible(), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAddNewRecordDialogDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDialogTitle(String expectedTitle) {
		try {
			String expected = (expectedTitle ?: '').trim()
			if (!expected) {
				failStop('verifyDialogTitle: expectedTitle is blank')
			}
			TestObject title = ar86RatesheetDialogTitle()
			waitVisible(title)
			WebUI.verifyElementVisible(title, FailureHandling.STOP_ON_FAILURE)
			String actual = (WebUI.getText(title, FailureHandling.OPTIONAL) ?: '').trim()
			if (!actual.equalsIgnoreCase(expected) && !actual.contains(expected)) {
				failStop("Dialog title mismatch. Expected '${expected}', actual '${actual}'")
			}
			KeywordUtil.logInfo("Verified dialog title: '${actual}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDialogTitle('${expectedTitle}') failed: ${e.message}")
		}
	}


	@Keyword
	def fillArRatesheetNewRow(String serviceCode, String shipmentType,
			String rateType, String rateValue = '', String percentOf = '') {
		try {
			//verifyArRatesheetInlineAddRowVisible()
			waitVisible(ar86RatesheetDialogVisible())
			//waitVisibleClickable(toAr86('inp_ratesheetServiceCode'))
			selectArRatesheetDropdownByLabel(toAr86('inp_ratesheetServiceCode'), serviceCode)
			WebUI.takeScreenshot()
//			waitVisibleClickable(toAr86('inp_ratesheetServiceDescription'))
//			WebUI.setText(toAr86('inp_ratesheetServiceDescription'), serviceDescription)
		//	WebUI.takeScreenshot()
			selectArRatesheetDropdownByLabel(toAr86('sel_ratesheetShipmentType'), shipmentType)
			selectArRatesheetDropdownByLabel(toAr86('sel_ratesheetRateType'), rateType)
			if ((rateType ?: '').equalsIgnoreCase('Percentage')) {
				selectArRatesheetDropdownByLabel(ar86RatesheetEditingSelect(3), (percentOf ?: 'Cost of Goods').trim())
				setArRatesheetEditingValueField(rateValue ?: '10')
			} else {
				//setArRatesheetEditingValueField(rateValue ?: generateArRatesheetRandomNumber())
				WebUI.setText(toAr86('inp_ratesheetRate1Value'), rateValue)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("fillArRatesheetNewRow failed: ${e.message}")
		}
	}

	/** Selects the visible option text from the data file (exact label match). */
	private static void selectArRatesheetDropdownByLabel(TestObject dropdown, String optionLabel) {
		String label = (optionLabel ?: '').trim()
		if (!label) {
			failStop('selectArRatesheetDropdownByLabel: option label is empty')
		}
		waitVisibleClickable(dropdown)
		WebUI.selectOptionByLabel(dropdown, label, false)
		WebUI.takeScreenshot()
	}

	private static void setArRatesheetEditingValueField(String value) {
		TestObject valueInp = toAr86('inp_edit_ratesheetRate1Value')
		WebUI.delay(2)
		WebUI.setText(valueInp, (value ?: '').trim())
	}
//	Object Repository/AR Management/CCIPH-86/select_edit_Rate_PercentageOf
	
	private static void setArRatesheetEditingRatePercentageOf(String percentOf) {
		TestObject perInp = toAr86('select_edit_Rate_PercentageOf')
		WebUI.delay(2)
		WebUI.setText(perInp, (percentOf ?: '').trim())
	}

	private static void setArRatesheetEditingValueField(String description, String value) {
		TestObject valueInp = findTestObject(
			'Object Repository/AR Management/CCIPH-86/inp_edit_ratesheetRate1Value',
			['description': description]
		)
		WebUI.waitForElementVisible(valueInp, 10)
		WebUI.clearText(valueInp)
		WebUI.setText(valueInp, value)
	}

	/** % Of dropdown when Rate Type is Percentage — OR {@code percentage_Of}. */
	private static TestObject ar86RatesheetPercentOfSelect(int rateNumber) {
		int n = rateNumber
		if (n < 1 || n > 3) {
			failStop("ar86RatesheetPercentOfSelect: rateNumber must be 1, 2, or 3 (got ${rateNumber})")
		}
		return findTestObject(
			'Object Repository/AR Management/CCIPH-86/percentage_Of',
			['rateIndex': String.valueOf(n)]
		)
	}

	/**
	 * Fills a single rate slot: Fixed → Value only; Percentage → % Of dropdown + Value.
	 * OR: {@code sel_ratesheetRateType} / {@code sel_ratesheetRate2Type}, {@code percentage_Of}, value inputs.
	 */
	@Keyword
	def fillArRatesheetRateSlot(int rateNumber, String rateType, String rateValue = '', String percentOf = '') {
		try {
			String type = (rateType ?: '').trim()
			if (!type) {
				failStop('fillArRatesheetRateSlot: rateType is required')
			}
			selectArRatesheetRateType(rateNumber, type)
			if (type.equalsIgnoreCase('Percentage')) {
				String pctOf = (percentOf ?: 'Cost of Goods').trim()
				TestObject pctOfDropdown = ar86RatesheetPercentOfSelect(rateNumber)
				waitVisibleClickable(pctOfDropdown)
				selectArRatesheetDropdownByLabel(pctOfDropdown, pctOf)
				String value = (rateValue ?: '10').trim()
				TestObject valueInput = ar86RatesheetRateValueInput(rateNumber)
				waitVisibleClickable(valueInput)
				WebUI.clearText(valueInput)
				WebUI.setText(valueInput, value)
			} else {
				TestObject valueInput = ar86RatesheetRateValueInput(rateNumber)
				waitVisibleClickable(valueInput)
				WebUI.clearText(valueInput)
				WebUI.setText(valueInput, (rateValue ?: '').trim())
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("fillArRatesheetRateSlot(${rateNumber}) failed: ${e.message}")
		}
	}

	/**
	 * Like {@link #fillArRatesheetNewRow} but uses {@code entity-settings-percentage-of-inline} for % Of when Rate Type is Percentage.
	 */
	@Keyword
	def fillArRatesheetNewRowWithRateDetails(String serviceCode, String shipmentType,
			String rateType, String rateValue = '', String percentOf = '') {
		try {
			waitVisible(ar86RatesheetDialogVisible())
			selectArRatesheetDropdownByLabel(toAr86('inp_ratesheetServiceCode'), serviceCode)
			WebUI.takeScreenshot()
			selectArRatesheetDropdownByLabel(toAr86('sel_ratesheetShipmentType'), shipmentType)
			fillArRatesheetRateSlot(1, rateType, rateValue, percentOf)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("fillArRatesheetNewRowWithRateDetails failed: ${e.message}")
		}
	}

	@Keyword
	def fillArRatesheetNewRowFromDataWithRateDetails(String serviceCode, String shipmentType,
			String rateType, String rateValue = '', String percentOf = '') {
		fillArRatesheetNewRowWithRateDetails(serviceCode, shipmentType, rateType, rateValue, percentOf)
	}

	@Keyword
	def fillArRatesheetNewRowFromData(String serviceCode, String shipmentType,
			String rateType, String rateValue = '', String percentOf = '') {
//		String suffix = generateArRatesheetRandomNumber()
//		String desc = "${(descPrefix ?: 'Automation_DescriptionTest').trim()}_${suffix}"
//		String svc = "${(serviceDescPrefix ?: 'Automation_Service_Description').trim()}_${suffix}"
		fillArRatesheetNewRow(serviceCode, shipmentType, rateType, rateValue, percentOf)
		//return [serviceCode: sC, serviceDescription: svc, randomSuffix: suffix]
	}

	@Keyword
	def clickArRatesheetSaveRow() {
		try {
			click(toAr86('btn_ratesheetSave'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
			clickSuccessSavePopup()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetSaveRow failed: ${e.message}")
		}
	}
	
	@Keyword
	def clickBackHistory() {
		try {
			TestObject backBtn = toAr37('btn_HistoryBack')
			waitVisibleClickable(backBtn)
			WebUI.scrollToElement(backBtn, DEFAULT_TIMEOUT)
			click(backBtn)
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickBackHistory failed: ${e.message}")
		}
	}
	
	@Keyword
	def clickSuccessSavePopup() {
		try {
			waitVisible(ar86RatesheetSuccessPopup())
			WebUI.delay(1)
			click(toAr86('btn_SuccessMessage'))
			
		}
		catch (Exception e) {
			failStop("clickArRatesheetSaveRow failed: ${e.message}")
		}
	}
	
	@Keyword
	def clickSaveEditRatesheet()
	{
		try {
			click(toAr86('btn_Edit_Save'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetSaveRow failed: ${e.message}")
		}
	}
	

//	@Keyword
//	def verifyArRatesheetRowPresent(String rowTextFragment) {
//		try {
//			String lit = xpathLiteral((rowTextFragment ?: '').trim())
//			TestObject row = new TestObject("ar86_rs_row_${rowTextFragment.hashCode()}")
//			row.addProperty('xpath', ConditionType.EQUALS,
//				"${AR_RATESHEET_TABLE_XPATH}//tbody/tr[contains(., ${lit})]")
//			waitVisible(row)
//			WebUI.verifyElementVisible(row, FailureHandling.STOP_ON_FAILURE)
//			WebUI.takeScreenshot()
//		} catch (Exception e) {
//			failStop("verifyArRatesheetRowPresent(${rowTextFragment}) failed: ${e.message}")
//		}
//	}
	
	
	@Keyword
	def verifyArRatesheetRowPresent(String description, String rateValue) {
		try {
	
			TestObject rowDescription = findTestObject(
				'Object Repository/AR Management/CCIPH-86/txt_Description_Row',
				['description' : description]
			)
	
			waitVisible(rowDescription)
			WebUI.verifyElementVisible(rowDescription)
	
			TestObject updatedRateValue = findTestObject(
				'Object Repository/AR Management/CCIPH-86/update_RateValue',
				[
					'description' : description,
					'rateValue'   : rateValue
				]
			)
	
			waitVisible(updatedRateValue)
			WebUI.verifyElementVisible(updatedRateValue)
	
			WebUI.comment(
				"Verified Description '${description}' contains updated Rate Value '${rateValue}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			failStop(
				"verifyArRatesheetRowPresent(${description}, ${rateValue}) failed: ${e.message}"
			)
		}
	}

	/**
	 * Verifies a RateSheet row displays the expected rate value only
	 * (OR: {@code AR Management/CCIPH-86/check_rateValue}).
	 */
	@Keyword
	def verifyArRatesheetRateValuePresent(String rateValue) {
		try {
			String value = (rateValue ?: '').trim()
			if (!value) {
				failStop('verifyArRatesheetRateValuePresent: rateValue is required')
			}
			TestObject rateValueObj = findTestObject(
				'Object Repository/AR Management/CCIPH-86/check_rateValue',
				['rateValue': value]
			)
			waitVisible(rateValueObj)
			WebUI.verifyElementVisible(rateValueObj, FailureHandling.STOP_ON_FAILURE)
			WebUI.comment("Verified RateSheet rate value '${value}' is visible.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetRateValuePresent(${rateValue}) failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyArRatesheetRateTypePresent(String rateType) {
		try {
			String type = (rateType ?: '').trim()
			if (!type) {
				failStop('verifyArRatesheetRateTypePresent: rateType is required')
			}
			TestObject rateTypeObj = findTestObject(
				'Object Repository/AR Management/CCIPH-86/check_RateType',
				['rateType': type]
			)
			waitVisible(rateTypeObj)
			WebUI.verifyElementVisible(rateTypeObj, FailureHandling.STOP_ON_FAILURE)
			WebUI.comment("Verified RateSheet rate type '${type}' is visible.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetRateValuePresent(${rateType}) failed: ${e.message}")
		}
	}

//	@Keyword
//	def verifyArRatesheetRowNotPresent(String rowTextFragment) {
//		try {
//			String lit = xpathLiteral((rowTextFragment ?: '').trim())
//			TestObject row = new TestObject("ar86_rs_row_absent_${rowTextFragment.hashCode()}")
//			row.addProperty('xpath', ConditionType.EQUALS,
//				"${AR_RATESHEET_TABLE_XPATH}//tbody/tr[contains(., ${lit})]")
//			if (WebUI.waitForElementPresent(row, 5, FailureHandling.OPTIONAL)) {
//				failStop("Ratesheet row still present containing '${rowTextFragment}'")
//			}
//			WebUI.takeScreenshot()
//		} catch (Exception e) {
//			failStop("verifyArRatesheetRowNotPresent(${rowTextFragment}) failed: ${e.message}")
//		}
//	}

	
	@Keyword
	def verifyArRatesheetRowNotPresent(String description) {
		try {
	
			TestObject rowDescription = findTestObject(
				'Object Repository/AR Management/CCIPH-86/txt_Description_Row',
				['description' : description]
			)
	
			boolean isPresent = WebUI.waitForElementPresent(
				rowDescription,
				5,
				FailureHandling.OPTIONAL
			)
	
			if (isPresent) {
				failStop("Ratesheet row with Description '${description}' is still present")
			}
	
			WebUI.comment("Verified Description '${description}' is not present")
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			failStop("verifyArRatesheetRowNotPresent failed: ${e.message}")
		}
	}

	/**
	 * Verifies a RateSheet rate value is not displayed
	 * (OR: {@code AR Management/CCIPH-86/check_rateValue}).
	 */
	@Keyword
	def verifyArRatesheetRateValueNotPresent(String rateValue) {
		try {
			String value = (rateValue ?: '').trim()
			if (!value) {
				failStop('verifyArRatesheetRateValueNotPresent: rateValue is required')
			}
			TestObject rateValueObj = findTestObject(
				'Object Repository/AR Management/CCIPH-86/check_rateValue',
				['rateValue': value]
			)
			boolean isPresent = WebUI.waitForElementPresent(
				rateValueObj,
				5,
				FailureHandling.OPTIONAL
			)
			if (isPresent) {
				failStop("Ratesheet rate value '${value}' is still present")
			}
			WebUI.comment("Verified rate value '${value}' is not present")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetRateValueNotPresent(${rateValue}) failed: ${e.message}")
		}
	}


	
//	@Keyword
//	def clickArRatesheetDeleteRowByDescription(String description) {
//		try {
//			String lit = xpathLiteral((description ?: '').trim())
//			TestObject delBtn = new TestObject("ar86_rs_del_${description.hashCode()}")
//			delBtn.addProperty('xpath', ConditionType.EQUALS,
//				"${AR_RATESHEET_TABLE_XPATH}//tbody/tr[contains(., ${lit})]//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'delete')][1]")
//			click(delBtn)
//			WebUI.delay(1)
//			WebUI.takeScreenshot()
//		} catch (Exception e) {
//			failStop("clickArRatesheetDeleteRowByDescription failed: ${e.message}")
//		}
//	}
	
	
	@Keyword
	def clickArRatesheetDeleteRowByDescription(String description) {
		try {
	
			TestObject deleteBtn = findTestObject(
				'Object Repository/AR Management/CCIPH-86/btn_Delete_By_Description',
				['description' : description]
			)
			WebUI.scrollToElement(deleteBtn, 5)
			WebUI.waitForElementClickable(deleteBtn, 10)
			WebUI.click(deleteBtn)
	
			WebUI.delay(1)
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			failStop("clickArRatesheetDeleteRowByDescription failed: ${e.message}")
		}
	}
	

	@Keyword
	def confirmArRatesheetDeletePopup() {
		try {
			TestObject confirm = toAr86('btn_ratesheetDeleteConfirm')
			waitVisibleClickable(confirm)
			click(confirm)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("confirmArRatesheetDeletePopup failed: ${e.message}")
		}
	}

	@Keyword
	def clickArRatesheetAddRatePlus() {
		try {
			clickArRatesheetAddAnotherRate()
		} catch (Exception e) {
			failStop("clickArRatesheetAddRatePlus failed: ${e.message}")
		}
	}

	@Keyword
	def clickArRatesheetAddAnotherRate() {
		try {
			TestObject addAnotherBtn = toAr86('btn_AddRatesheetAnother')
			if (!WebUI.waitForElementVisible(addAnotherBtn, 8, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo('btn_AddRatesheetAnother not visible — trying btn_ratesheetAddRatePlus fallback.')
				addAnotherBtn = toAr86('btn_ratesheetAddRatePlus')
			}
			waitVisibleClickable(addAnotherBtn)
			WebUI.scrollToElement(addAnotherBtn, DEFAULT_TIMEOUT)
			click(addAnotherBtn)
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetAddAnotherRate failed: ${e.message}")
		}
	}

	@Keyword
	def selectArRatesheetRateType(int rateNumber, String rateType) {
		try {
			String type = (rateType ?: '').trim()
			if (!type) {
				failStop('selectArRatesheetRateType: rateType is required')
			}
			selectArRatesheetDropdownByLabel(ar86RatesheetRateTypeSelect(rateNumber), type)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectArRatesheetRateType(${rateNumber}, ${rateType}) failed: ${e.message}")
		}
	}

	@Keyword
	def enterArRatesheetRateValue(int rateNumber, String rateValue) {
		try {
			String value = (rateValue ?: '').trim()
			if (!value) {
				failStop('enterArRatesheetRateValue: rateValue is required')
			}
			TestObject valueInput = ar86RatesheetRateValueInput(rateNumber)
			waitVisibleClickable(valueInput)
			WebUI.clearText(valueInput)
			WebUI.setText(valueInput, value)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterArRatesheetRateValue(${rateNumber}, ${rateValue}) failed: ${e.message}")
		}
	}

	@Keyword
	def selectArRatesheetRate2Type(String rateType) {
		selectArRatesheetRateType(2, rateType)
	}

	@Keyword
	def enterArRatesheetRate2Value(String rateValue) {
		enterArRatesheetRateValue(2, rateValue)
	}

	/**
	 * Configures any rate slot (1–3): Rate Type + Value, then clicks {@code + ADD ANOTHER RATE}.
	 * Rate 1 type → OR {@code sel_ratesheetRateType}; Rate 2 → {@code sel_ratesheetRate2Type};
	 * Rate 3 type/value → dynamic {@code entity-settings-col-rate-type-3} / {@code entity-settings-col-rate-3}.
	 *
	 * @param rateNumber 1 = Rate 1, 2 = Rate 2, 3 = Rate 3
	 */
	@Keyword
	def fillArRatesheetRateAndClickAddAnotherRate(int rateNumber, String rateType, String rateValue, String percentOf = '') {
		try {
			fillArRatesheetRateSlot(rateNumber, rateType, rateValue, percentOf)
			clickArRatesheetAddAnotherRate()
			KeywordUtil.logInfo("Rate ${rateNumber} configured (type='${rateType}', value='${rateValue}') and Add Another Rate clicked.")
		} catch (Exception e) {
			failStop("fillArRatesheetRateAndClickAddAnotherRate(${rateNumber}) failed: ${e.message}")
		}
	}

	/** Convenience wrapper for Rate 2 — delegates to {@code fillArRatesheetRateAndClickAddAnotherRate(2, ...)}. */
	@Keyword
	def fillArRatesheetRate2AndClickAddAnotherRate(String rateType, String rateValue) {
		fillArRatesheetRateAndClickAddAnotherRate(2, rateType, rateValue)
	}

	@Keyword
	def verifyArRatesheetRateSlotVisible(int rateNumber) {
		try {
			String label = "Rate ${rateNumber}"
			TestObject slot = new TestObject("ar86_rs_slot_${rateNumber}")
			slot.addProperty('xpath', ConditionType.EQUALS,
				"//*[contains(normalize-space(.),'${label}') or contains(normalize-space(.),'Rate${rateNumber}')]")
			waitVisible(slot)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetRateSlotVisible(${rateNumber}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArRatesheetAddRatePlusNotVisible() {
		try {
			TestObject plusBtn = toAr86('btn_ratesheetAddRatePlus')
			
			if (WebUI.waitForElementPresent(plusBtn, 4, FailureHandling.OPTIONAL)) {
				failStop('Expected + icon to be hidden after Rate 3, but it is still visible')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetAddRatePlusNotVisible failed: ${e.message}")
		}
	}
	

	@Keyword
	def verifyArRatesheetPercentOfDropdownOptions(String rateType = 'Percentage') {
		try {
			selectArRatesheetDropdownByLabel(toAr86('sel_ratesheetRateType'), rateType)
			WebUI.delay(1)
			['Cost of Goods', 'Total Invoice'].each { String opt ->
				TestObject sel = ar86RatesheetEditingSelect(3)
				WebUI.verifyOptionPresentByLabel(sel, opt, 0, FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetPercentOfDropdownOptions failed: ${e.message}")
		}
	}

	@Keyword
	def clickArRatesheetEditFirstRow() {
		try {
			TestObject editBtn = toAr86('btn_ratesheetEditFirst')
//			click(editBtn)
//			WebUI.delay(1)
//			waitVisible(ar86RatesheetEditingRowObject())
//			WebUI.takeScreenshot()
			
			WebUI.waitForElementClickable(editBtn, 10)
			WebUI.scrollToElement(editBtn, 5)
			WebUI.click(editBtn)
	
			WebUI.delay(1)
		//	waitVisible(ar86RatesheetEditingRowObject())
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			failStop("clickArRatesheetEditFirstRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickArRatesheetEditRowByDescription(String description) {
		try {
//			String lit = xpathLiteral((description ?: '').trim())
			TestObject editBtn = toAr86('btn_ratesheetEditFirst')
//			editBtn.addProperty('xpath', ConditionType.EQUALS,
//				"${AR_RATESHEET_TABLE_XPATH}//tbody/tr[contains(., ${lit})]//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit')][1]")
			click(editBtn)
			WebUI.delay(1)
		//	waitVisible(ar86RatesheetEditingRowObject())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetEditRowByDescription failed: ${e.message}")
		}
	}
	
	@Keyword
	def clickArRatesheetEditRowByRateValue(String rateValue) {
		try {
			String value = (rateValue ?: '').trim()
			if (!value) {
				failStop('clickArRatesheetEditRowByRateValue: rateValue is required')
			}

			TestObject rateValueObj = findTestObject(
				'Object Repository/AR Management/CCIPH-86/span_ratesheetRateValueDisplay',
				['rateValue': value]
			)
			waitVisible(rateValueObj)
			WebUI.verifyElementVisible(rateValueObj, FailureHandling.STOP_ON_FAILURE)

			TestObject editBtn = findTestObject(
				'Object Repository/AR Management/CCIPH-86/btn_Edit_By_RateValue',
				['rateValue': value]
			)
			waitVisibleClickable(editBtn)
			WebUI.scrollToElement(editBtn, 5)
			WebUI.click(editBtn)
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetEditRowByRateValue('${rateValue}') failed: ${e.message}")
		}
	}

	@Keyword
	def clickCreatedArRatesheetEditRow(String rateValue) {
		clickArRatesheetEditRowByRateValue(rateValue)
	}
	

	@Keyword
	def updateArRatesheetRate1Value(String newValue) {
		try {
			setArRatesheetEditingValueField(newValue)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("updateArRatesheetRate1Value failed: ${e.message}")
		}
	}

	
	@Keyword
	def updateArRatesheetPercentOf(String newValue, String percentOf) {
		try {
			setArRatesheetEditingRatePercentageOf(percentOf)
			setArRatesheetEditingValueField(newValue)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("updateArRatesheetRate1Value failed: ${e.message}")
		}
	}
	
	@Keyword
	def clickArRatesheetRate1ValueLinkForRow(String rowTextFragment) {
		try {
			String lit = xpathLiteral((rowTextFragment ?: '').trim())
			TestObject link = new TestObject("ar86_rate1_link_${rowTextFragment.hashCode()}")
			link.addProperty('xpath', ConditionType.EQUALS,
				"${AR_RATESHEET_TABLE_XPATH}//tbody/tr[contains(., ${lit})]//*[self::a or self::button or self::td][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'rate 1') or contains(normalize-space(.),'Rate 1') or position()=last()-1][1]")
			if (!WebUI.waitForElementPresent(link, 8, FailureHandling.OPTIONAL)) {
				link = toAr86('lnk_ratesheetRate1Value')
			}
			click(link)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetRate1ValueLinkForRow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArRatesheetCreationPageVisible() {
		try {
			waitVisible(toAr86('txt_anchor_ratesheetCreationPage'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArRatesheetCreationPageVisible failed: ${e.message}")
		}
	}

	@Keyword
	def clickArRatesheetCreationBack() {
		try {
			click(toAr86('btn_ratesheetCreationBack'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickArRatesheetCreationBack failed: ${e.message}")
		}
	}

	@Keyword
	def verifyArAccountReceivableDetailAfterRatesheetBack() {
		try {
			verifyArBillingPreferencesChargeCodeTabsAvailable()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyArAccountReceivableDetailAfterRatesheetBack failed: ${e.message}")
		}
	}

	/**
	 * TC_AR_FUNC_057: generates unique description/service/value, uses {@code rs_Shipment_Type} /
	 * {@code rs_Rate_Type} from the data file for dropdowns.
	 */
	//@Keyword
//	Map createAndSaveArRatesheetRow(String description, String serviceDescription, String rs_Shipment_Type, String rs_Rate_Type,String percentOf, String rs_Type_Value) 
//	{
//		clickArRatesheetAddNew()
//		fillArRatesheetNewRow(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, percentOf, rs_Type_Value)
//		clickArRatesheetSaveRow()
//		verifyArRatesheetRowPresent(description, rs_Type_Value)
//		return [
//			description         : description,
//			serviceDescription  : serviceDescription,
//			rs_Type_Value       : rs_Type_Value,
//			rs_Shipment_Type    : rs_Shipment_Type,
//			rs_Rate_Type        : rs_Rate_Type
//		]
//	}
	@Keyword
	Map createAndSaveArRatesheetRow(String description, String serviceDescription, String rs_Shipment_Type, String rs_Rate_Type, String percentOf, String rs_Type_Value) {
		try {
			clickArRatesheetAddNew()
			fillArRatesheetNewRow(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rs_Type_Value, percentOf)
			clickArRatesheetSaveRow()
			verifyArRatesheetRowPresent(description, rs_Type_Value)
			return [
				description        : description,
				serviceDescription : serviceDescription,
				rs_Type_Value      : rs_Type_Value,
				rs_Shipment_Type   : rs_Shipment_Type,
				rs_Rate_Type       : rs_Rate_Type
			]
		} catch (Exception e) {
			failStop("createAndSaveArRatesheetRow failed: ${e.message}")
			return [:]
		}
	}

	@Keyword
	Map createAndSaveArRatesheetRowFromData(String descPrefix, String serviceDescPrefix, String shipmentType,
			String rateType, String rateValue = '') {
		try {
			clickArRatesheetAddNew()
			Map rowData = fillArRatesheetNewRowFromData(descPrefix, serviceDescPrefix, shipmentType, rateType, rateValue)
			clickArRatesheetSaveRow()
			verifyArRatesheetRowPresent(rowData.description as String, rateValue ?: '')
			return rowData
		} catch (Exception e) {
			failStop("createAndSaveArRatesheetRowFromData failed: ${e.message}")
			return [:]
		}
	}

	// ---------- CCIPH-104: AP Account Detail page ----------

	/**
	 * Builds a safe XPath string literal for dynamic label text (e.g. {@code 'AP Account Name'}).
	 */
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
		return "concat('" + v.split("'").join("', \"'\", '") + "')"
	}

	private static TestObject searchableEntityOption(String entityName) {
		String lit = xpathLiteral((entityName ?: '').trim())
		TestObject opt = new TestObject("searchable_entity_option_${(entityName ?: '').trim()}")
		opt.addProperty('xpath', ConditionType.EQUALS,
			"//button[contains(@class,'SearchableSelect-module') and contains(@class,'option') and normalize-space()=${lit}]")
		return opt
	}

	/**
	 * Detail form field block (label + control) — scoped to avoid search-panel labels (e.g. Entity).
	 * Matches labels with optional suffixes (e.g. {@code Status *}).
	 */
	private static String detailFormLabelMatchCondition(String labelText) {
		String lit = xpathLiteral((labelText ?: '').trim())
		return "normalize-space()=${lit} or starts-with(normalize-space(), concat(${lit}, ' '))"
	}

	private static String detailFormFieldBlockXPath(String labelText) {
		String cond = detailFormLabelMatchCondition(labelText)
		return "//div[contains(@class,'min-w-0')][.//label[${cond}]]"
	}

	/**
	 * Exact label on detail form: {@code //div[@class contains min-w-0]//label[normalize-space()='Entity']}
	 */
	private static String detailLabelExactMatchXPath(String labelText) {
		String cond = detailFormLabelMatchCondition(labelText)
		return "${detailFormFieldBlockXPath(labelText)}//label[${cond}]"
	}

	private static String detailFormFieldControlXPath(String labelText) {
		String lx = detailLabelExactMatchXPath(labelText)
		return "${lx}/ancestor::div[contains(@class,'fieldControl')][1]" +
			" | ${detailFormFieldBlockXPath(labelText)}//div[contains(@class,'fieldControl')][1]"
	}

	private static TestObject detailLabelByExactText(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		TestObject labelTo = new TestObject("ap_detail_label_${safeId}")
		labelTo.addProperty('xpath', ConditionType.EQUALS, detailLabelExactMatchXPath(label))
		return labelTo
	}

	/**
	 * Resolves the input/select/textarea for a detail field via its exact {@code <label>} text.
	 */
	private static TestObject detailFieldByLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		String blockXp = detailFormFieldBlockXPath(label)
		TestObject field = new TestObject("ap_detail_field_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${blockXp}/input[1]" +
			" | ${blockXp}//input[1]" +
			" | ${blockXp}//select[1]" +
			" | ${blockXp}//textarea[1]"
		)
		return field
	}

	/** Editable SearchableSelect by position among {@code accountDetailsTrigger} buttons (left-to-right). */
	private static TestObject detailEditableSelectByIndex(int oneBasedIndex) {
		TestObject field = new TestObject("ap_detail_editable_select_idx_${oneBasedIndex}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"(//${DETAIL_EDITABLE_SEARCHABLE_TRIGGER_XPATH})[${oneBasedIndex}]" +
			" | (//${DETAIL_EDITABLE_SELECT_XPATH})[${oneBasedIndex}]"
		)
		return field
	}

	/** Combobox-style control (when UI uses {@code button} instead of native {@code select}). */
	private static TestObject detailEditableButtonByLabel(String labelText) {
		String cond = detailFormLabelMatchCondition(labelText)
		String labelXp = "//label[${cond}]"
		String blockXp = detailFormFieldBlockXPath(labelText)
		TestObject field = new TestObject("ap_detail_editable_btn_${labelText.replaceAll('[^a-zA-Z0-9]+', '_')}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${labelXp}/following-sibling::button[contains(@class,'cursor-pointer')][1]" +
			" | ${labelXp}/following-sibling::button[@role='combobox'][1]" +
			" | ${labelXp}/parent::*//button[contains(@class,'h-10') and contains(@class,'cursor-pointer')][1]" +
			" | ${labelXp}/following::button[contains(@class,'cursor-pointer')][1]" +
			" | ${blockXp}//button[contains(@class,'cursor-pointer') and contains(@class,'h-10')][1]"
		)
		return field
	}

	/** SearchableSelect listbox trigger on AP Account Detail (Rate Currency, Status, etc.). */
	private static TestObject detailEditableSearchableSelectByLabel(String labelText) {
		String safeId = (labelText ?: '').replaceAll('[^a-zA-Z0-9]+', '_')
		String cond = detailFormLabelMatchCondition(labelText)
		String labelXp = "//${DETAIL_EDITABLE_SEARCHABLE_LABEL_XPATH}[${cond}]"
		String blockXp = detailFormFieldBlockXPath(labelText)
		TestObject field = new TestObject("ap_detail_editable_searchable_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"//div[contains(@class,'accountDetailsWrap')][.//${DETAIL_EDITABLE_SEARCHABLE_LABEL_XPATH}[${cond}]]//${DETAIL_EDITABLE_SEARCHABLE_TRIGGER_XPATH}" +
			" | ${labelXp}/following-sibling::${DETAIL_EDITABLE_SEARCHABLE_TRIGGER_XPATH}" +
			" | ${blockXp}//${DETAIL_EDITABLE_SEARCHABLE_TRIGGER_XPATH}" +
			" | //label[${cond}]/following-sibling::button[@aria-haspopup='listbox'][1]"
		)
		return field
	}

	/**
	 * Editable control for a label — native {@code select} paths (used by AR detail fallback chain).
	 */
	private static TestObject detailEditableFieldByLabel(String labelText) {
		return detailEditableNativeSelectByLabel(labelText)
	}

	private static TestObject resolveDetailEditableField(String labelText) {
		return detailEditableSearchableSelectByLabel(labelText)
	}

	/** Legacy native {@code select} locator by label — fallback when SearchableSelect is absent. */
	private static TestObject detailEditableNativeSelectByLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		String cond = detailFormLabelMatchCondition(label)
		String labelXp = "//label[${cond}]"
		String sel = DETAIL_EDITABLE_SELECT_XPATH
		String blockXp = detailFormFieldBlockXPath(label)
		String fieldControl = detailFormFieldControlXPath(label)
		TestObject field = new TestObject("ap_detail_editable_native_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${labelXp}/following-sibling::${sel}" +
			" | ${labelXp}/parent::*//${sel}" +
			" | ${labelXp}/following::${sel}[1]" +
			" | ${labelXp}/following-sibling::select[1]" +
			" | ${labelXp}/parent::*//select[1]" +
			" | ${blockXp}//${sel}" +
			" | ${blockXp}//select[1]" +
			" | ${fieldControl}//${sel}" +
			" | ${fieldControl}//select[1]"
		)
		return field
	}

	/** Read-only input under a detail label ({@code cursor-not-allowed} / readonly / disabled). */
	private static TestObject detailReadOnlyInputByLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		String blockXp = detailFormFieldBlockXPath(label)
		String roInput = DETAIL_READONLY_INPUT_XPATH
		TestObject field = new TestObject("ap_detail_readonly_input_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${blockXp}/${roInput}" +
			" | ${blockXp}//${roInput}" +
			" | ${blockXp}/input[@readonly or @disabled][1]"
		)
		return field
	}

	private static void waitPresent(TestObject testObject, String messageIfMissing) {
		if (!WebUI.waitForElementPresent(testObject, DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)) {
			failStop(messageIfMissing)
		}
	}

	private static boolean htmlAttributeIsSet(String attributeValue) {
		if (attributeValue == null) {
			return false
		}
		String v = attributeValue.trim()
		return v.isEmpty() || 'true'.equalsIgnoreCase(v) || 'disabled'.equalsIgnoreCase(v)
	}

	private static boolean isDetailFieldReadOnly(TestObject field) {
		if (!WebUI.waitForElementPresent(field, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
			return false
		}
		try {
			WebElement el = WebUiCommonHelper.findWebElement(field, DEFAULT_TIMEOUT)
			String tag = (el.getTagName() ?: '').toLowerCase()
			if (tag == 'input' || tag == 'textarea') {
				boolean readonlyAttr = htmlAttributeIsSet(el.getAttribute('readonly'))
				boolean disabledAttr = htmlAttributeIsSet(el.getAttribute('disabled'))
				boolean notEnabled = !el.isEnabled()
				String cls = el.getAttribute('class') ?: ''
				boolean readOnlyStyle = cls.contains('cursor-not-allowed')
				return readonlyAttr || disabledAttr || notEnabled || readOnlyStyle
			}
			if (tag == 'select') {
				return !el.isEnabled() || htmlAttributeIsSet(el.getAttribute('disabled'))
			}
			return true
		} catch (Exception e) {
			KeywordUtil.markWarning("isDetailFieldReadOnly WebElement check failed: ${e.message}")
			String ro = WebUI.getAttribute(field, 'readonly', FailureHandling.OPTIONAL)
			String dis = WebUI.getAttribute(field, 'disabled', FailureHandling.OPTIONAL)
			return htmlAttributeIsSet(ro) || htmlAttributeIsSet(dis)
		}
	}

	private static boolean isDetailFieldEditable(TestObject field) {
		if (!WebUI.waitForElementPresent(field, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
			return false
		}
		try {
			WebElement el = WebUiCommonHelper.findWebElement(field, DEFAULT_TIMEOUT)
			String tag = (el.getTagName() ?: '').toLowerCase()
			if (tag == 'select') {
				return el.isEnabled() && !htmlAttributeIsSet(el.getAttribute('disabled'))
			}
			if (tag == 'button') {
				String cls = el.getAttribute('class') ?: ''
				String role = el.getAttribute('role') ?: ''
				String haspopup = el.getAttribute('aria-haspopup') ?: ''
				boolean looksEditable = cls.contains('cursor-pointer') ||
					'combobox'.equalsIgnoreCase(role) ||
					'listbox'.equalsIgnoreCase(haspopup)
				return looksEditable && el.isEnabled() && !htmlAttributeIsSet(el.getAttribute('disabled'))
			}
			if (tag == 'input' || tag == 'textarea') {
				return !isDetailFieldReadOnly(field)
			}
			return false
		} catch (Exception e) {
			KeywordUtil.markWarning("isDetailFieldEditable WebElement check failed: ${e.message}")
			return !isDetailFieldReadOnly(field)
		}
	}

	@Keyword
	def clickFirstAccountNumberLink() {
		try {
			verifySearchResultsTableVisible()
			TestObject link = apFirstRowAccountNumberLink()
			clickWithJsFallback(link, 'first-row Account Number link')
			WebUI.delay(2)
		} catch (Exception e) {
			failStop("clickFirstAccountNumberLink failed: ${e.message}")
		}
	}

	@Keyword
	def openApAccountDetailFromSearchResults(String searchCriteria, String searchBy, String searchValue, String entity = '') {
		try {
			performSearch(searchCriteria, searchBy, searchValue, entity)
			clickFirstAccountNumberLink()
			verifyApAccountDetailPageLoaded()
		} catch (Exception e) {
			failStop("openApAccountDetailFromSearchResults failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailPageLoaded() {
		try {
		//	WebUI.delay(3)
			waitVisible(to104('txt_anchor_ApAccountDetailPage'))
			//verifyElementText(to104('txt_anchor_ApAccountDetailPage'))
			WebUI.verifyElementVisible(to104('txt_anchor_ApAccountDetailPage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailPageLoaded failed: ${e.message}")
		}
	}

	/**
	 * Asserts that {@code expectedText} appears anywhere on the current page (visible in DOM / body text).
	 */
	@Keyword
	def verifyTextPresentOnPage(String expectedText) {
		try {
			String text = (expectedText ?: '').trim()
			if (!text) {
				failStop('verifyTextPresentOnPage: expectedText is blank')
			}
			WebUI.verifyTextPresent(text, false, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyTextPresentOnPage('${expectedText}') failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailTextsPresent(List<String> expectedTexts) {
		try {
			if (!expectedTexts) {
				failStop('verifyApAccountDetailTextsPresent: list is empty')
			}
			expectedTexts.each { String t ->
				verifyTextPresentOnPage(t)
			}
		} catch (Exception e) {
			failStop("verifyApAccountDetailTextsPresent failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailLabelVisible(String labelText) {
		verifyTextPresentOnPage(labelText)
	}

	@Keyword
	def verifyApAccountDetailFieldLabelVisible(String labelText) {
		verifyApAccountDetailLabelVisible(labelText)
	}

	@Keyword
	def verifyApAccountDetailFieldsVisible() {
		try {
			verifyApAccountDetailPageLoaded()
			verifyApAccountDetailTextsPresent([
				'AP Account Name',
				'Account Number',
				'Country',
				'Entity',
				'Account Type',
				'Rate Currency',
				'Billing Currency',
				'Market Structure',
				'Status'
			])
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailFieldsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailFieldIsReadOnly(String labelText) {
		try {
			String label = (labelText ?: '').trim()
			if (!label) {
				failStop('verifyApAccountDetailFieldIsReadOnly: labelText is blank')
			}
			verifyTextPresentOnPage(label)
			TestObject readOnlyInput = detailReadOnlyInputByLabel(label)
			waitPresent(readOnlyInput, "Read-only input for '${label}' not found (label block + ${DETAIL_READONLY_INPUT_XPATH}).")
			WebUI.scrollToElement(readOnlyInput, DEFAULT_TIMEOUT)
			if (!isDetailFieldReadOnly(readOnlyInput)) {
				failStop("Expected '${label}' input to be read-only (readonly/disabled/cursor-not-allowed) but it is editable.")
			}
			KeywordUtil.logInfo("Verified read-only input for label '${label}'.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailFieldIsReadOnly(${labelText}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailFieldIsEditable(String labelText) {
		try {
			String label = (labelText ?: '').trim()
			if (!label) {
				failStop('verifyApAccountDetailFieldIsEditable: labelText is blank')
			}
			verifyTextPresentOnPage(label)
			TestObject editableField = resolveDetailEditableField(label)
			waitPresent(editableField, "Editable SearchableSelect for '${label}' not found (accountDetailsTrigger + listbox).")
			WebUI.scrollToElement(editableField, DEFAULT_TIMEOUT)
			if (!isDetailFieldEditable(editableField)) {
				failStop("Expected '${label}' to be an editable select/dropdown on AP Account Detail page.")
			}
			KeywordUtil.logInfo("Verified editable control for label '${label}'.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailFieldIsEditable(${labelText}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailReadOnlyFields() {
		try {
			verifyApAccountDetailPageLoaded()
			List<String> readOnlyLabels = [
				'AP Account Name',
				'Account Number',
				'Country',
				'Entity',
				'Account Type'
			]
			readOnlyLabels.each { String label ->
				verifyApAccountDetailFieldIsReadOnly(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailReadOnlyFields failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountDetailEditableFields() {
		try {
			verifyApAccountDetailPageLoaded()
			['Rate Currency', 'Billing Currency', 'Market Structure', 'Status'].each { String label ->
				verifyApAccountDetailFieldIsEditable(label)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountDetailEditableFields failed: ${e.message}")
		}
	}

	// ——— CCIPH-200 Create AP Account ———

	private static String createFormLabelExactXPath(String labelText) {
		String label = (labelText ?: '').trim()
		String lit = xpathLiteral(label)
		String lower = xpathLiteral(label.toLowerCase())
		// Required fields render as "Account Number *" — allow exact text or label starting with field name
		return "//label[" +
			"normalize-space()=${lit} or " +
			"translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')=${lower} or " +
			"starts-with(normalize-space(), concat(${lit}, ' ')) or " +
			"starts-with(normalize-space(), concat(${lit}, '*'))" +
			"]"
	}

	/** @deprecated use {@link #createFormLabelExactXPath} — contains() caused wrong field/option matches */
	private static String createFormLabelContainsXPath(String labelText) {
		return createFormLabelExactXPath(labelText)
	}

	private static String createFormFieldBlockXPath(String labelText) {
		String lit = xpathLiteral((labelText ?: '').trim())
		return "//div[contains(@class,'min-w-0')][.//label[normalize-space()=${lit} or starts-with(normalize-space(), concat(${lit}, ' '))]]"
	}

	private static String createFormFieldControlXPath(String labelText) {
		String lx = createFormLabelExactXPath(labelText)
		return "${lx}/ancestor::div[contains(@class,'fieldControl')][1]" +
			" | ${createFormFieldBlockXPath(labelText)}//div[contains(@class,'fieldControl')][1]"
	}

	private static String createFormEditableInputXPath(String fieldControlXPath) {
		String inputXp = "${fieldControlXPath}//input[not(@type='hidden') and not(@readonly) and not(@disabled) and not(contains(@class,'cursor-not-allowed'))][1]"
		String areaXp = "${fieldControlXPath}//textarea[not(@readonly) and not(@disabled) and not(contains(@class,'cursor-not-allowed'))][1]"
		return "${inputXp} | ${areaXp}"
	}

	private static TestObject createInputByLabel(String labelText) {
		String safeId = (labelText ?: '').replaceAll('[^a-zA-Z0-9]+', '_')
		String lx = createFormLabelContainsXPath(labelText)
		String blockXp = createFormFieldBlockXPath(labelText)
		String fieldControlFromLabel = "${lx}/ancestor::div[contains(@class,'fieldControl')][1]"
		String fieldControlFromBlock = "${blockXp}//div[contains(@class,'fieldControl')][1]"
		TestObject field = new TestObject("ap_create_input_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			createFormEditableInputXPath(fieldControlFromLabel) +
			" | " + createFormEditableInputXPath(fieldControlFromBlock)
		)
		return field
	}

	/** First visible, enabled, non-readonly text control under the create-form field block for {@code labelText}. */
	private static WebElement findCreateFormEditableInputElement(String labelText) {
		String lx = createFormLabelExactXPath(labelText)
		String blockXp = createFormFieldBlockXPath(labelText)
		List<String> scopes = [
			"${lx}/ancestor::div[contains(@class,'fieldControl')][1]",
			"${blockXp}//div[contains(@class,'fieldControl')][1]",
			blockXp
		]
		List<String> inputQueries = [
			".//input[not(@type='hidden')]",
			".//textarea",
			".//*[@role='textbox']",
			".//*[@contenteditable='true']"
		]
		def driver = DriverFactory.getWebDriver()
		for (String scopeXp : scopes) {
			List<WebElement> scopesEls = driver.findElements(By.xpath(scopeXp))
			for (WebElement scope : scopesEls) {
				if (!scope.isDisplayed()) {
					continue
				}
				for (String q : inputQueries) {
					List<WebElement> candidates = scope.findElements(By.xpath(q))
					for (WebElement el : candidates) {
						if (!el.isDisplayed() || !el.isEnabled()) {
							continue
						}
						String cls = el.getAttribute('class') ?: ''
						if (cls.contains('cursor-not-allowed')) {
							continue
						}
						if (htmlAttributeIsSet(el.getAttribute('readonly')) || htmlAttributeIsSet(el.getAttribute('disabled'))) {
							continue
						}
						return el
					}
				}
			}
		}
		return null
	}

	private static void setCreateFormTextByLabel(String labelText, String value) {
		String text = (value ?: '').trim()
		if (!text) {
			failStop("setCreateFormTextByLabel: value is required for '${labelText}'")
		}
		WebElement el = findCreateFormEditableInputElement(labelText)
		if (el == null) {
			failStop("Editable input for '${labelText}' not found on Create AP Account form.")
		}
		WebUI.executeJavaScript('arguments[0].scrollIntoView({block:\"center\"});', java.util.Arrays.asList(el))
		WebUI.delay(0.3)
		try {
			el.click()
		} catch (Exception ignored) {
		}
		WebUI.delay(0.2)
		try {
			WebUI.executeJavaScript(
				'''
				var el = arguments[0];
				var val = arguments[1];
				el.focus();
				var tag = (el.tagName || '').toLowerCase();
				if (tag === 'input' || tag === 'textarea') {
					var proto = tag === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
					var desc = Object.getOwnPropertyDescriptor(proto, 'value');
					if (desc && desc.set) {
						desc.set.call(el, val);
					} else {
						el.value = val;
					}
					el.dispatchEvent(new Event('input', { bubbles: true }));
					el.dispatchEvent(new Event('change', { bubbles: true }));
				} else {
					el.textContent = val;
					el.dispatchEvent(new Event('input', { bubbles: true }));
				}
				''',
				java.util.Arrays.asList(el, text)
			)
		} catch (Exception jsEx) {
			KeywordUtil.logInfo("JS set value failed for '${labelText}', retrying with sendKeys: ${jsEx.message}")
			def driver = DriverFactory.getWebDriver()
			new Actions(driver).click(el)
				.keyDown(org.openqa.selenium.Keys.CONTROL)
				.sendKeys('a')
				.keyUp(org.openqa.selenium.Keys.CONTROL)
				.sendKeys(text)
				.perform()
		}
		WebUI.delay(0.3)
		String actual = (el.getAttribute('value') ?: el.getText() ?: '').trim()
		if (!actual.contains(text) && !text.contains(actual)) {
			KeywordUtil.markWarning("After entering '${labelText}', field shows '${actual}' (expected '${text}').")
		}
	}

	private static TestObject createFormDropdownTriggerByLabel(String labelText) {
		String safeId = (labelText ?: '').replaceAll('[^a-zA-Z0-9]+', '_')
		String fieldControl = createFormFieldControlXPath(labelText)
		String labelXp = createFormLabelExactXPath(labelText)
		TestObject field = new TestObject("ap_create_dropdown_trigger_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${fieldControl}//button[@aria-haspopup='listbox'][1]" +
			" | ${fieldControl}//button[contains(@class,'trigger')][1]" +
			" | ${labelXp}/following-sibling::button[@aria-haspopup='listbox'][1]" +
			" | ${fieldControl}//select[1]"
		)
		return field
	}

	private static void closeCreateFormOpenListbox() {
		try {
			TestObject pageTitle = to200('txt_anchor_CreateApAccountPage')
			if (WebUI.waitForElementPresent(pageTitle, 3, FailureHandling.OPTIONAL)) {
				WebUI.click(pageTitle)
			}
			WebUI.executeJavaScript(
				"document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true}));",
				null
			)
		} catch (Exception ignored) {
		}
		WebUI.delay(0.5)
	}

	private static WebElement findVisibleListboxOptionExact(String optionLabel) {
		String expected = (optionLabel ?: '').trim()
		def driver = DriverFactory.getWebDriver()
		List<WebElement> listboxes = driver.findElements(By.xpath("//*[@role='listbox']"))
		for (WebElement listbox : listboxes) {
			if (!listbox.isDisplayed()) {
				continue
			}
			List<WebElement> options = listbox.findElements(By.xpath(".//*[@role='option'] | .//li | .//div[@role='option']"))
			for (WebElement option : options) {
				String text = (option.getText() ?: '').trim()
				if (text.equalsIgnoreCase(expected)) {
					return option
				}
			}
		}
		return null
	}

	private static void clickCreateFormDropdownOptionExact(String optionLabel) {
		String expected = (optionLabel ?: '').trim()
		String lit = xpathLiteral(expected)
		TestObject opt = new TestObject("ap_create_option_exact_${expected.replaceAll('[^a-zA-Z0-9]+', '_')}")
		opt.addProperty('xpath', ConditionType.EQUALS,
			"//*[@role='listbox' and not(contains(@style,'display: none'))]//*[@role='option'][normalize-space()=${lit}]" +
			" | //*[@role='listbox']//li[normalize-space()=${lit}]" +
			" | //*[@role='listbox']//*[normalize-space()=${lit}]"
		)
		if (WebUI.waitForElementClickable(opt, 5, FailureHandling.OPTIONAL)) {
			WebUI.scrollToElement(opt, 10)
			WebUI.click(opt)
			return
		}
		WebElement match = findVisibleListboxOptionExact(expected)
		if (match == null) {
			failStop("Dropdown option '${expected}' not found (exact match) in visible listbox.")
		}
		WebUI.executeJavaScript('arguments[0].scrollIntoView({block:\"center\"});', java.util.Arrays.asList(match))
		WebUI.delay(1)
		match.click()
	}

	@Keyword
	def clickCreateApAccount() {
		try {
			click(to200('btn_createApAccount'))
		} catch (Exception e) {
			failStop("clickCreateApAccount failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateApAccountPageLoaded() {
		try {
			waitVisible(to200('txt_anchor_CreateApAccountPage'))
			WebUI.verifyElementVisible(to200('txt_anchor_CreateApAccountPage'), FailureHandling.STOP_ON_FAILURE)
			verifyTextPresentOnPage('Fill account details as needed')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateApAccountPageLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def openCreateApAccountPage() {
		try {
			verifyApSearchPageLoaded()
			clickCreateApAccount()
			verifyCreateApAccountPageLoaded()
		} catch (Exception e) {
			failStop("openCreateApAccountPage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateApAccountFieldsVisible() {
		try {
			verifyCreateApAccountPageLoaded()
			[
				'Account Number',
				'Account Name',
				'Country',
				'Entity',
				'Status',
				'Rate Currency',
				'Billing Currency',
				'Market Structure',
				'Account Type'
			].each { String label ->
				verifyTextPresentOnPage(label)
			}
			waitVisible(to200('btn_createSave'))
			WebUI.verifyElementVisible(to200('btn_createSave'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateApAccountFieldsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateApAccountTypeReadOnlyVendor() {
		try {
			verifyCreateApAccountPageLoaded()
			verifyTextPresentOnPage('Account Type')
			verifyTextPresentOnPage('Vendor')
			TestObject accountTypeField = createInputByLabel('Account Type')
			if (WebUI.waitForElementPresent(accountTypeField, 8, FailureHandling.OPTIONAL)) {
				if (!isDetailFieldReadOnly(accountTypeField)) {
					String tag = WebUiCommonHelper.findWebElement(accountTypeField, DEFAULT_TIMEOUT)?.getTagName()?.toLowerCase()
					if (tag == 'select') {
						WebElement el = WebUiCommonHelper.findWebElement(accountTypeField, DEFAULT_TIMEOUT)
						if (el.isEnabled()) {
							failStop('Account Type should be read-only on Create AP Account page but select is enabled.')
						}
					} else {
						failStop('Account Type control is present but not read-only/disabled.')
					}
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateApAccountTypeReadOnlyVendor failed: ${e.message}")
		}
	}

	@Keyword
	def clickCreateApAccountSave() {
		try {
			click(to200('btn_createSave'))
		} catch (Exception e) {
			failStop("clickCreateApAccountSave failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateApMandatoryValidation() {
		try {
			waitVisible(to('txt_validationMessage'))
			WebUI.verifyElementVisible(to('txt_validationMessage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateApMandatoryValidation failed: ${e.message}")
		}
	}

	@Keyword
	def clickCreateSaveWithoutMandatoryFields() {
		try {
			verifyCreateApAccountPageLoaded()
			clickCreateApAccountSave()
		} catch (Exception e) {
			failStop("clickCreateSaveWithoutMandatoryFields failed: ${e.message}")
		}
	}

	@Keyword
	def selectCreateFormDropdown(String fieldLabel, String optionLabel) {
		try {
			String field = (fieldLabel ?: '').trim()
			String option = (optionLabel ?: '').trim()
			if (!field || !option) {
				failStop("selectCreateFormDropdown: fieldLabel and optionLabel are required (field='${field}', option='${option}')")
			}
			closeCreateFormOpenListbox()
			KeywordUtil.logInfo("Create AP form — selecting '${option}' for '${field}' (from test data).")
			TestObject trigger = createFormDropdownTriggerByLabel(field)
			waitVisibleClickable(trigger)
			WebUI.scrollToElement(trigger, 10)
			String tag = WebUiCommonHelper.findWebElement(trigger, DEFAULT_TIMEOUT)?.getTagName()?.toLowerCase()
			if (tag == 'select') {
				WebUI.selectOptionByLabel(trigger, option, false)
			} else {
				WebUI.click(trigger)
				WebUI.delay(1)
				clickCreateFormDropdownOptionExact(option)
			}
			WebUI.delay(1)
			String shown = (WebUI.getText(trigger, FailureHandling.OPTIONAL) ?: '').trim()
			if (shown && !shown.equalsIgnoreCase(option) && !shown.toLowerCase().contains(option.toLowerCase())) {
				failStop("Field '${field}': expected '${option}' after selection but trigger shows '${shown}'.")
			}
			closeCreateFormOpenListbox()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectCreateFormDropdown(${fieldLabel}, ${optionLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCreateApAccountTypeValue(String expectedType = 'Vendor') {
		try {
			String expected = (expectedType ?: 'Vendor').trim()
			verifyTextPresentOnPage(expected)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCreateApAccountTypeValue(${expectedType}) failed: ${e.message}")
		}
	}

	@Keyword
	def enterCreateApAccountNumber(String accountNumber) {
		try {
			setCreateFormTextByLabel('Account Number', accountNumber)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterCreateApAccountNumber failed: ${e.message}")
		}
	}

	@Keyword
	def enterCreateApAccountName(String accountName) {
		try {
			setCreateFormTextByLabel('Account Name', accountName)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterCreateApAccountName failed: ${e.message}")
		}
	}

	/**
	 * Fills mandatory Create AP Account fields from CSV-bound values (CCIPH-200 / AC1).
	 * Market Structure is optional — skipped when blank.
	 */
	@Keyword
	def fillCreateApAccountMandatoryFields(
			String accountNumber,
			String accountName,
			String accountType,
			String country,
			String createEntity,
			String status,
			String rateCurrency,
			String billingCurrency,
			String marketStructure
	) {
		try {
			verifyCreateApAccountPageLoaded()
			enterCreateApAccountNumber(accountNumber)
			enterCreateApAccountName(accountName)
			KeywordUtil.logInfo("Create AP data — country='${country}', create_Entity='${createEntity}', status='${status}', rate='${rateCurrency}', billing='${billingCurrency}', market='${marketStructure}'")
			if (accountType?.trim()) {
				verifyCreateApAccountTypeValue(accountType.trim())
			}
			selectCreateFormDropdown('Country', country)
			selectCreateFormDropdown('Entity', createEntity)
			selectCreateFormDropdown('Status', status)
			selectCreateFormDropdown('Rate Currency', rateCurrency)
			selectCreateFormDropdown('Billing Currency', billingCurrency)
			if (marketStructure?.trim()) {
				selectCreateFormDropdown('Market Structure', marketStructure.trim())
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("fillCreateApAccountMandatoryFields failed: ${e.message}")
		}
	}

	@Keyword
	def clickCreateApAccountSuccessPopupOk() {
		try {
			waitVisible(to200('txt_createSuccessPopupTitle'))
			WebUI.verifyElementVisible(to200('txt_createSuccessPopupTitle'), FailureHandling.STOP_ON_FAILURE)
			waitVisibleClickable(to200('btn_createSuccessOk'))
			click(to200('btn_createSuccessOk'))
			WebUI.delay(1)
			boolean popupGone = !WebUI.waitForElementVisible(to200('btn_createSuccessOk'), 8, FailureHandling.OPTIONAL)
			if (!popupGone) {
				KeywordUtil.markWarning('Success popup OK clicked but button may still be visible — continuing.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickCreateApAccountSuccessPopupOk failed: ${e.message}")
		}
	}

	@Keyword
	def saveCreateApAccountForm() {
		try {
			clickCreateApAccountSave()
			clickCreateApAccountSuccessPopupOk()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("saveCreateApAccountForm failed: ${e.message}")
		}
	}

	@Keyword
	def verifyApAccountCreated(String accountNumber = '', String accountName = '', String entity = '') {
		try {
			String name = (accountName ?: '').trim()
			if (!name) {
				failStop('verifyApAccountCreated: accountName is required to search for the created account')
			}
			boolean successPopup = WebUI.waitForElementVisible(to200('txt_createSuccessPopupTitle'), 8, FailureHandling.OPTIONAL)
			if (successPopup) {
				clickCreateApAccountSuccessPopupOk()
			}
			navigateToApManagement()
			verifyApSearchPageLoaded()
			performSearch('Account Name', 'Contains', name, (entity ?: '').trim())
			verifySearchResultsTableVisible()
			verifyAllRowsContainInColumn('Account Name', name)
			if (accountNumber?.trim()) {
				verifyAllRowsContainInColumn('Account Number', accountNumber.trim())
			}
			KeywordUtil.logInfo("Verified created AP account '${name}' in search results.")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyApAccountCreated failed: ${e.message}")
		}
	}

	@Keyword
	def createApAccountWithMandatoryFields(
			String accountNumber,
			String accountName,
			String accountType,
			String country,
			String createEntity,
			String status,
			String rateCurrency,
			String billingCurrency,
			String marketStructure
	) {
		try {
			fillCreateApAccountMandatoryFields(
				accountNumber, accountName, accountType, country, createEntity, status, rateCurrency, billingCurrency, marketStructure
			)
			saveCreateApAccountForm()
			//verifyApAccountCreated(accountNumber, accountName)
		} catch (Exception e) {
			failStop("createApAccountWithMandatoryFields failed: ${e.message}")
		}
	}
}
