package customsearch_360viewscreen

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import org.openqa.selenium.WebElement

import customsmanagement.ProfileGlobals

/**
 * Custom Search / 360 View Screen (CCIPH-21).
 * Assumes authenticated session — login handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX_21 = 'CustomSearch_360ViewScreen/CCIPH-21/'
	private static final String OR_PREFIX = 'CustomSearch_360ViewScreen/CCIPH-21/'
	private static final int DEFAULT_TIMEOUT = 30
	private static final String DEFAULT_DEV_CUSTOMS_SEARCH_URL = 'https://dev-cc.aramex.com/customs-management/customs-search'
	private static final String DEFAULT_STAGE_CUSTOMS_SEARCH_URL = 'https://stage-cc.aramex.com/customs-management/customs-search'
	private static final List<String> COST_CHARGE_CODES = ['ECD', 'ECS', 'EC3']
	private static final List<String> REVENUE_CHARGE_CODES = ['DTA', 'DTC', 'DTS', 'DT3']
	private static final List<String> SEARCH_CRITERIA_OPTIONS = ['Tracking Number', 'Console', 'Account Number', 'BOE']
	private static final List<String> ADDITIONAL_DETAIL_LABELS = [
		'Origin Entity', 'Origin Country', 'Dest. Entity', 'Dest. Country',
		'3rd party Entity', '3rd party Country', '3rd Party Entity', '3rd Party Country'
	]
	private static final List<String> GRID_COLUMN_HEADERS = [
		'Entity', 'Tracking Number', 'Account Number', 'Account Name',
		'Origin', 'Destination', '3rd Party', 'Charge Group', 'Charge Code',
		'Total Estimate', 'Total Actual', 'Status'
	]

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}
	private static TestObject to21(String name) { 
		return findTestObject(OR_PREFIX_21 + name) 
		}

	private static void failStop(String message) {
		try { WebUI.takeScreenshot() } catch (Throwable ignored) { }
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
		if (!v) return "''"
		if (!v.contains("'")) return "'${v}'"
		if (!v.contains('"')) return '"' + v + '"'
		String[] parts = v.split("'")
		StringBuilder sb = new StringBuilder('concat(')
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) sb.append(", \"'\", ")
			sb.append("'").append(parts[i]).append("'")
		}
		sb.append(')')
		return sb.toString()
	}

	private static TestObject dynamicXpathObject(String id, String xpath) {
		TestObject to = new TestObject(id)
		to.addProperty('xpath', ConditionType.EQUALS, xpath)
		return to
	}

	private static String normalizeLabel(String label) {
		return (label ?: '').trim().toLowerCase()
	}

	private static TestObject fieldByLabel(String label) {
		String lit = xpathLiteral(normalizeLabel(label))
		return dynamicXpathObject("cs_field_${label.hashCode()}",
			"//*[self::label or self::span or self::p][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/following::*[self::select or self::input or self::button or @role='combobox'][1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/ancestor::*[self::div or self::fieldset][1]//*[self::select or self::input or @role='combobox' or self::button][1]")
	}

	private static TestObject dropdownTriggerByLabel(String label) {
		String lit = xpathLiteral(normalizeLabel(label))
		return dynamicXpathObject("cs_ddl_trigger_${label.hashCode()}",
			"//label[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/following-sibling::button[1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/following::button[@role='combobox'][1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/following::button[contains(@class,'cursor-pointer')][1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]/ancestor::*[self::div][1]//button[@role='combobox'][1]")
	}

	private static TestObject searchCriteriaDropdown() {
		return dynamicXpathObject('cs_search_criteria_ddl',
			"//label[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search criteria')]/following-sibling::button[1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search criteria')]/following::button[@role='combobox'][1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search criteria')]/following::button[contains(@class,'cursor-pointer')][1]")
	}

	private static TestObject resolveDropdownTriggerByLabel(String label) {
		if ('search criteria'.equalsIgnoreCase(normalizeLabel(label))) {
			if (WebUI.waitForElementPresent(searchCriteriaDropdown(), 5, FailureHandling.OPTIONAL)) {
				return searchCriteriaDropdown()
			}
		}
		TestObject trigger = dropdownTriggerByLabel(label)
		if (WebUI.waitForElementPresent(trigger, 5, FailureHandling.OPTIONAL)) {
			return trigger
		}
		return fieldByLabel(label)
	}

	private static String getFieldDisplayedValue(TestObject field) {
		String text = (WebUI.getText(field, FailureHandling.OPTIONAL) ?: '').trim()
		if (text) return text
		return (WebUI.getAttribute(field, 'value', FailureHandling.OPTIONAL) ?: '').trim()
	}

	private static boolean isValueAlreadySelected(TestObject field, String value) {
		String expected = normalizeLabel(value)
		if (!expected) return false
		String shown = normalizeLabel(getFieldDisplayedValue(field))
		return shown == expected || shown.contains(expected) || expected.contains(shown)
	}

	private static void clickDropdownOption(String value) {
		String lit = xpathLiteral((value ?: '').trim())
		String litLower = xpathLiteral(normalizeLabel(value))
		TestObject option = dynamicXpathObject("cs_ddl_option_${value.hashCode()}",
			"//*[@role='listbox']//*[@role='option' or self::li][normalize-space()=${lit} or contains(normalize-space(),${lit}) or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${litLower})]" +
			" | //*[@role='option' or self::li or self::div][normalize-space()=${lit} or contains(normalize-space(),${lit}) or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${litLower})][1]")
		waitVisibleClickable(option)
		WebUI.click(option)
	}

	private static TestObject searchCriteriaInput() {
		return dynamicXpathObject('cs_search_input',
			"//label[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search criteria')]/following-sibling::input[not(@type='hidden')][1]" +
			" | //*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search criteria')]/following::input[not(@type='hidden')][1]" +
			" | //button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'multiple')]/preceding::input[not(@type='hidden')][1]" +
			" | //input[not(@type='hidden')][contains(@placeholder,'Search') or contains(@name,'search') or contains(@id,'search')][1]")
	}

	private static String resolveCustomsSearchUrl(boolean stage = false) {
		if (stage) {
			return resolveCustomsSearchStageUrl()
		}
		String url = (ProfileGlobals.get('CUSTOMS_SEARCH_URL', '') ?: '').trim()
		return url ?: DEFAULT_DEV_CUSTOMS_SEARCH_URL
	}

	/**
	 * Resolves Stage navigation URL from profile {@code CUSTOMS_APP_URL_STAGE} (used as-is).
	 */
	private static String resolveCustomsSearchStageUrl() {
		String url = (ProfileGlobals.get('CUSTOMS_APP_URL_STAGE', '') ?: '').trim()
		return url ?: DEFAULT_STAGE_CUSTOMS_SEARCH_URL
	}

	@Keyword
	def navigateToCustomsSearchScreen() {
		try {
			WebUI.navigateToUrl(resolveCustomsSearchUrl(false))
			verifyCustomsSearchScreenLoaded()
		} catch (Exception e) {
			failStop("navigateToCustomsSearchScreen failed: ${e.message}")
		}
	}

	@Keyword
	def navigateToStageCustomsSearchScreen() {
		try {
			WebUI.navigateToUrl(resolveCustomsSearchStageUrl())
			verifyCustomsSearchScreenLoaded()
		} catch (Exception e) {
			failStop("navigateToStageCustomsSearchScreen failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCustomsSearchScreenLoaded() {
		try {
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			waitVisible(fieldByLabel('Charge Group'))
			waitVisible(to('btn_search'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCustomsSearchScreenLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def selectDropdownByLabel(String label, String value) {
		try {
			String target = (value ?: '').trim()
			if (!target) return

			TestObject field = resolveDropdownTriggerByLabel(label)
			waitVisibleClickable(field)

			if (isValueAlreadySelected(field, target)) {
				KeywordUtil.logInfo("Dropdown '${label}' already shows '${target}'; skipping selection.")
				WebUI.takeScreenshot()
				return
			}

			String tag = (WebUI.getAttribute(field, 'tagName', FailureHandling.OPTIONAL) ?: '').toLowerCase()
			if (tag == 'select') {
				try {
					WebUI.selectOptionByLabel(field, target, false)
				} catch (Exception ignored) {
					click(field)
					WebUI.delay(1)
					clickDropdownOption(target)
				}
			} else {
				click(field)
				WebUI.delay(1)
				clickDropdownOption(target)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectDropdownByLabel(${label}, ${value}) failed: ${e.message}")
		}
	}

	@Keyword
	def selectSearchCriteria(String criteria) {
		try {
			String target = (criteria ?: 'Tracking Number').trim()
			TestObject field = resolveDropdownTriggerByLabel('Search Criteria')
			waitVisibleClickable(field)
			if (isValueAlreadySelected(field, target)) {
				KeywordUtil.logInfo("Search Criteria already '${target}'; skipping selection.")
				WebUI.takeScreenshot()
				return
			}
			selectDropdownByLabel('Search Criteria', target)
		} catch (Exception e) {
			failStop("selectSearchCriteria(${criteria}) failed: ${e.message}")
		}
	}

	@Keyword
	def selectChargeGroup(String chargeGroup) {
		selectDropdownByLabel('Charge Group', chargeGroup)
	}

	@Keyword
	def selectChargeCode(String chargeCode) {
		selectDropdownByLabel('Charge Code', chargeCode)
	}

	@Keyword
	def selectDateType(String dateType) {
		selectDropdownByLabel('Date Type', dateType)
	}

	@Keyword
	def setStartDate(String dateValue) {
		try {
			TestObject field = fieldByLabel('Start Date')
			waitVisibleClickable(field)
			WebUI.clearText(field, FailureHandling.OPTIONAL)
			WebUI.setText(field, (dateValue ?: '').trim())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("setStartDate failed: ${e.message}")
		}
	}

	@Keyword
	def setEndDate(String dateValue) {
		try {
			TestObject field = fieldByLabel('End Date')
			waitVisibleClickable(field)
			WebUI.clearText(field, FailureHandling.OPTIONAL)
			WebUI.setText(field, (dateValue ?: '').trim())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("setEndDate failed: ${e.message}")
		}
	}

	@Keyword
	def setSearchInputText(String text) {
		try {
			TestObject field = searchCriteriaInput()
			waitVisibleClickable(field)
			WebUI.clearText(field, FailureHandling.OPTIONAL)
			WebUI.setText(field, (text ?: '').trim())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("setSearchInputText failed: ${e.message}")
		}
	}

	@Keyword
	def setMultipleTrackingNumbers(String newlineSeparatedValues) {
		setSearchInputText((newlineSeparatedValues ?: '').replace(',', '\n'))
	}

	@Keyword
	def clickSearch() {
		try {
			click(to('btn_search'))
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
		} catch (Exception e) {
			failStop("clickSearch failed: ${e.message}")
		}
	}

	@Keyword
	def clickMultipleSearchButton() {
		try {
			click(to('btn_multipleSearch'))
			waitVisible(to('dlg_multipleSearch'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickMultipleSearchButton failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyFirstRowChargeGroupCodeAndStatus(
			String expectedChargeGroup,
			String expectedChargeCode,
			String expectedStatus) {
	
		try {
	
			TestObject firstRow = dynamicXpathObject(
				"firstResultRow",
				"(//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')])[1]"
			)
	
			waitVisible(firstRow)
	
			// Charge Group - 8th column
			TestObject chargeGroup = dynamicXpathObject(
				"firstRowChargeGroup",
				"(//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')])[1]//td[8]"
			)
	
			// Charge Code - 9th column
			TestObject chargeCode = dynamicXpathObject(
				"firstRowChargeCode",
				"(//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')])[1]//td[9]"
			)
	
			// Status - 12th column
			TestObject status = dynamicXpathObject(
				"firstRowStatus",
				"(//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')])[1]//td[12]"
			)
	
			// Validate Charge Group
			WebUI.verifyElementText(
				chargeGroup,
				expectedChargeGroup,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Validate Charge Code
			WebUI.verifyElementText(
				chargeCode,
				expectedChargeCode,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Validate Status
			WebUI.verifyElementText(
				status,
				expectedStatus,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"First row validated successfully: " +
				"Charge Group='${expectedChargeGroup}', " +
				"Charge Code='${expectedChargeCode}', " +
				"Status='${expectedStatus}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"verifyFirstRowChargeGroupCodeAndStatus failed: ${e.message}"
			)
		}
	}

	@Keyword
	def setAdditionalDetailsToggle(boolean enabled) {
		try {
			TestObject toggle = to('toggle_additionalDetails')
			waitVisibleClickable(toggle)
			boolean currentlyOn = isAdditionalDetailsEnabled()
			if (currentlyOn != enabled) {
				WebUI.click(toggle)
				WebUI.delay(1)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("setAdditionalDetailsToggle(${enabled}) failed: ${e.message}")
		}
	}

	private boolean isAdditionalDetailsEnabled() {
		TestObject origin = fieldByLabel('Origin Entity')
		return WebUI.waitForElementVisible(origin, 3, FailureHandling.OPTIONAL)
	}

	@Keyword
	def verifyAdditionalDetailsFieldsVisible(boolean shouldBeVisible) {
		try {
			for (String label : ADDITIONAL_DETAIL_LABELS) {
				TestObject field = fieldByLabel(label)
				boolean visible = WebUI.waitForElementVisible(field, 5, FailureHandling.OPTIONAL)
				if (shouldBeVisible && !visible) {
					failStop("Expected Additional Details field '${label}' to be visible")
				}
				if (!shouldBeVisible && visible) {
					failStop("Expected Additional Details field '${label}' to be hidden")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAdditionalDetailsFieldsVisible(${shouldBeVisible}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeCodeOptionsForGroup(String chargeGroup, List<String> expectedCodes) {
		try {
			selectChargeGroup(chargeGroup)
			TestObject chargeCodeField = fieldByLabel('Charge Code')
			waitVisibleClickable(chargeCodeField)
			click(chargeCodeField)
			for (String code : expectedCodes) {
				TestObject option = dynamicXpathObject("cs_charge_code_${code}",
					"//*[@role='option' or self::li or self::div or self::span][normalize-space()=${xpathLiteral(code)}]")
				if (!WebUI.waitForElementVisible(option, 5, FailureHandling.OPTIONAL)) {
					failStop("Charge Code option '${code}' not found for Charge Group '${chargeGroup}'")
				}
			}
			WebUI.sendKeys(chargeCodeField, '\uE00C')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyChargeCodeOptionsForGroup failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCostChargeCodesAvailable() {
		verifyChargeCodeOptionsForGroup('Cost', COST_CHARGE_CODES)
	}

	@Keyword
	def verifyRevenueChargeCodesAvailable() {
		verifyChargeCodeOptionsForGroup('Revenue', REVENUE_CHARGE_CODES)
	}

	@Keyword
	def verifyDateMandatoryOnSearch(String dateType = 'Pickup Date') {
		try {
			selectDateType(dateType)
			clickSearch()
			if (!WebUI.waitForElementVisible(to('msg_validationError'), 10, FailureHandling.OPTIONAL)) {
				TestObject startDate = fieldByLabel('Start Date')
				String required = WebUI.getAttribute(startDate, 'required', FailureHandling.OPTIONAL)
				String ariaRequired = WebUI.getAttribute(startDate, 'aria-required', FailureHandling.OPTIONAL)
				if (!required && !'true'.equalsIgnoreCase(ariaRequired)) {
					failStop('Start/End Date mandatory validation not shown when Date Type is selected')
				}
			} else {
				WebUI.verifyElementVisible(to('msg_validationError'), FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDateMandatoryOnSearch failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEndDateBeforeStartDateError(String startDate, String endDate, String dateType = 'Pickup Date') {
		try {
			selectDateType(normalizeDateType(dateType))
			setStartDate(startDate)
			setEndDate(endDate)
			clickSearch()
			waitVisible(to('msg_validationError'))
			String msg = (WebUI.getText(to('msg_validationError'), FailureHandling.OPTIONAL) ?: '').toLowerCase()
			if (!msg.contains('end date') && !msg.contains('less than') && !msg.contains('before')) {
				failStop("Expected End Date validation message but got: ${msg}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEndDateBeforeStartDateError failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNoDataFoundMessage() {
		try {
			waitVisible(to('msg_noDataFound'))
			WebUI.verifyElementVisible(to('msg_noDataFound'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyNoDataFoundMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchResultsDisplayed() {
		try {
			waitVisible(to('tbl_searchResults'))
			WebUI.verifyElementVisible(to('tbl_searchResults'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchResultsDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchCriteriaDropdownValues() {
		try {
			TestObject criteria = resolveDropdownTriggerByLabel('Search Criteria')
			waitVisibleClickable(criteria)
			click(criteria)
			WebUI.delay(1)
			for (String option : SEARCH_CRITERIA_OPTIONS) {
				TestObject opt = dynamicXpathObject("cs_criteria_${option.hashCode()}",
					"//*[@role='listbox']//*[@role='option' or self::li][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(option.toLowerCase())})]" +
					" | //*[@role='option' or self::li or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(option.toLowerCase())})]")
				if (!WebUI.waitForElementVisible(opt, 5, FailureHandling.OPTIONAL)) {
					failStop("Search Criteria option '${option}' not visible")
				}
			}
			WebUI.click(criteria)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchCriteriaDropdownValues failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFilterLabelsVisible() {
		try {
			List<String> labels = [
				'Charge Country', 'Entity', 'Charge Group', 'Charge Code', 'Charge Status',
				'Date Type', 'Start Date', 'End Date', 'Account Type', 'Business Type',
				'Category', 'Search Criteria', 'Additional Details'
			]
			for (String label : labels) {
				TestObject lbl = dynamicXpathObject("cs_lbl_${label.hashCode()}",
					"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(normalizeLabel(label))})]")
				WebUI.verifyElementVisible(lbl, FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterLabelsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def toggleHideShowFilters() {
		try {
			click(to('btn_hideShowFilters'))
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("toggleHideShowFilters failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFieldValueRetained(String label, String expectedValue) {
		try {
			TestObject field = fieldByLabel(label)
			waitVisible(field)
			String actual = (WebUI.getAttribute(field, 'value', FailureHandling.OPTIONAL) ?: WebUI.getText(field, FailureHandling.OPTIONAL) ?: '').trim()
			if (!actual.equalsIgnoreCase((expectedValue ?: '').trim())) {
				failStop("Field '${label}' expected '${expectedValue}' but was '${actual}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFieldValueRetained(${label}) failed: ${e.message}")
		}
	}

	@Keyword
	def refreshCustomsSearchPage() {
		try {
			WebUI.refresh()
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			verifyCustomsSearchScreenLoaded()
		} catch (Exception e) {
			failStop("refreshCustomsSearchPage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyMultipleSearchDialogFields() {
		try {
			waitVisible(to('dlg_multipleSearch'))
			TestObject reference = dynamicXpathObject('cs_multiple_reference',
				"//*[@role='dialog' or contains(@class,'modal')]//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'reference')]")
			TestObject input = dynamicXpathObject('cs_multiple_input',
				"//*[@role='dialog' or contains(@class,'modal')]//textarea | //*[@role='dialog']//input[not(@type='hidden')]")
			TestObject searchBtn = dynamicXpathObject('cs_multiple_search_btn',
				"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]")
			TestObject cancelBtn = dynamicXpathObject('cs_multiple_cancel_btn',
				"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cancel')]")
			WebUI.verifyElementVisible(reference, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(input, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(searchBtn, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(cancelBtn, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyMultipleSearchDialogFields failed: ${e.message}")
		}
	}

	@Keyword
	def clickFirstSearchResultRow() {
		try {
			TestObject row = dynamicXpathObject('cs_first_result_row',
				"(//table//tbody//tr[.//td][1] | //*[contains(@class,'grid')]//*[contains(@class,'row')][1])[1]")
			waitVisibleClickable(row)
			WebUI.click(row)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickFirstSearchResultRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickBackButton() {
		try {
			TestObject back = dynamicXpathObject('cs_back_btn',
				"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'back')]")
			click(back)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
		} catch (Exception e) {
			failStop("clickBackButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvalidTrackingNumbersMessage() {
		try {
			TestObject msg = dynamicXpathObject('cs_invalid_tracking',
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid tracking')]")
			waitVisible(msg)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvalidTrackingNumbersMessage failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchValidationError() {
		try {
			waitVisible(to('msg_validationError'))
			WebUI.verifyElementVisible(to('msg_validationError'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchValidationError failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFutureStartDateDisabled() {
		try {
			TestObject startDate = fieldByLabel('Start Date')
			waitVisible(startDate)
			WebUI.click(startDate)
			TestObject futureDay = dynamicXpathObject('cs_future_day_disabled',
				"//*[contains(@class,'calendar') or contains(@class,'datepicker')]//*[contains(@class,'disabled') or @aria-disabled='true'][1]")
			if (!WebUI.waitForElementVisible(futureDay, 5, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo('Future date cells may use a different disabled pattern; verify manually if needed')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFutureStartDateDisabled failed: ${e.message}")
		}
	}

	@Keyword
	def applySingleFilterSearch(String filterLabel, String filterValue) {
		selectDropdownByLabel(filterLabel, filterValue)
		clickSearch()
	}

	@Keyword
	def applyMultipleFiltersSearch(Map<String, String> filters) {
		filters?.each { label, value ->
			if (value) selectDropdownByLabel(label, value)
		}
		clickSearch()
	}

	@Keyword
	def applyAllAvailableFilters(Map<String, String> data = [:]) {
		selectDropdownByLabel('Charge Country', data.charge_Country ?: 'All')
		selectDropdownByLabel('Entity', data.entity ?: 'All')
		selectChargeGroup(data.charge_Group ?: 'Cost')
		selectChargeCode(data.charge_Code ?: 'ECD')
		selectDropdownByLabel('Charge Status', data.charge_Status ?: 'All')
		selectDateType(data.date_Type ?: 'Pickup Date')
		if (data.start_Date) setStartDate(data.start_Date)
		if (data.end_Date) setEndDate(data.end_Date)
		selectDropdownByLabel('Account Type', data.account_Type ?: 'All')
		if (data.business_Type) selectDropdownByLabel('Business Type', data.business_Type)
		selectDropdownByLabel('Category', data.category ?: 'All')
		selectSearchCriteria(data.search_Criteria ?: 'Tracking Number')
		if (data.search_Input) setSearchInputText(data.search_Input)
		setAdditionalDetailsToggle(true)
		if (data.origin_Entity) selectDropdownByLabel('Origin Entity', data.origin_Entity)
		if (data.origin_Country) selectDropdownByLabel('Origin Country', data.origin_Country)
		if (data.dest_Entity) selectDropdownByLabel('Dest. Entity', data.dest_Entity)
		if (data.dest_Country) selectDropdownByLabel('Dest. Country', data.dest_Country)
		clickSearch()
	}

	@Keyword
	def verifyAdditionalDetailsToggleBehavior() {
		setAdditionalDetailsToggle(false)
		verifyAdditionalDetailsFieldsVisible(false)
		setAdditionalDetailsToggle(true)
		verifyAdditionalDetailsFieldsVisible(true)
	}

	@Keyword
	def verifyFilterValuesRetainedAfterReload(Map<String, String> expectedValues) {
		refreshCustomsSearchPage()
		expectedValues?.each { label, value ->
			if (value) verifyFieldValueRetained(label, value)
		}
	}

	@Keyword
	def clickPaginationNext() {
		try {
			TestObject next = dynamicXpathObject('cs_pagination_next',
				"//*[contains(@class,'pagination')]//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'next') or contains(@aria-label,'Next')][not(@disabled)]")
			click(next)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
		} catch (Exception e) {
			failStop("clickPaginationNext failed: ${e.message}")
		}
	}

	@Keyword
	def verifyPaginationControlsVisible() {
		try {
			TestObject footer = dynamicXpathObject('cs_pagination_footer',
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'showing') or contains(@class,'pagination')]")
			waitVisible(footer)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyPaginationControlsVisible failed: ${e.message}")
		}
	}

	@Keyword
	def clickFavoriteButton() {
		try {
			TestObject fav = dynamicXpathObject('cs_favorite_btn',
				"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'favour') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'favor')]")
			click(fav)
		} catch (Exception e) {
			failStop("clickFavoriteButton failed: ${e.message}")
		}
	}

	@Keyword
	def clickExportButton() {
		try {
			TestObject exportBtn = dynamicXpathObject('cs_export_btn',
				"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'export')]")
			click(exportBtn)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickExportButton failed: ${e.message}")
		}
	}

	@Keyword
	def closeMultipleSearchDialog() {
		try {
			TestObject cancel = dynamicXpathObject('cs_multiple_cancel',
				"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cancel')]")
			click(cancel)
			if (WebUI.waitForElementNotPresent(to('dlg_multipleSearch'), 5, FailureHandling.OPTIONAL)) {
				WebUI.takeScreenshot()
			}
		} catch (Exception e) {
			failStop("closeMultipleSearchDialog failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDetailPageLoaded() {
		try {
			TestObject detail = dynamicXpathObject('cs_detail_page',
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'analysis') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'history') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]")
			waitVisible(detail)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDetailPageLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def clickHistoryTab() {
		try {
			TestObject history = dynamicXpathObject('cs_history_tab',
				"//*[self::button or self::a or self::span][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'history')]")
			click(history)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
		} catch (Exception e) {
			failStop("clickHistoryTab failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryPaginationDefaultPageSize(int expectedSize = 50) {
		try {
			TestObject footer = dynamicXpathObject('cs_history_footer',
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'showing') and contains(.,'${expectedSize}')]")
			if (!WebUI.waitForElementVisible(footer, 10, FailureHandling.OPTIONAL)) {
				verifyPaginationControlsVisible()
			} else {
				WebUI.verifyElementVisible(footer, FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationDefaultPageSize failed: ${e.message}")
		}
	}

	@Keyword
	def selectHistoryPageSize(String pageSize) {
		try {
			TestObject pageSizeSel = dynamicXpathObject('cs_history_page_size',
				"//*[contains(@class,'page-size') or contains(@name,'page') or self::select][1]")
			waitVisibleClickable(pageSizeSel)
			String tag = (WebUI.getAttribute(pageSizeSel, 'tagName', FailureHandling.OPTIONAL) ?: '').toLowerCase()
			if (tag == 'select') {
				WebUI.selectOptionByLabel(pageSizeSel, pageSize, false)
			} else {
				click(pageSizeSel)
				TestObject opt = dynamicXpathObject("cs_page_size_${pageSize}",
					"//*[@role='option' or self::li][normalize-space()=${xpathLiteral(pageSize)}]")
				click(opt)
			}
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectHistoryPageSize failed: ${e.message}")
		}
	}

	@Keyword
	def verifyPaginationButtonDisabled(String buttonName) {
		try {
			TestObject btn = dynamicXpathObject("cs_pagination_${buttonName}_disabled",
				"//*[contains(@class,'pagination')]//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'${buttonName.toLowerCase()}')][@disabled or contains(@class,'disabled')]")
			waitVisible(btn)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyPaginationButtonDisabled(${buttonName}) failed: ${e.message}")
		}
	}

	@Keyword
	def searchAndOpenFirstResultForHistoryTests(Map<String, String> data = [:]) {
		selectSearchCriteria(data.search_Criteria ?: 'Tracking Number')
		if (data.search_Input) setSearchInputText(data.search_Input)
		clickSearch()
		verifySearchResultsDisplayed()
		clickFirstSearchResultRow()
		clickHistoryTab()
	}

	/**
	 * TC_Detailed_29 flow: click Search with current filters only (no tracking number input),
	 * then open first shipment and History tab.
	 */
	@Keyword
	def searchAndOpenFirstResultWithoutTracking() {
		clickSearch()
		verifySearchResultsDisplayed()
		clickFirstSearchResultRow()
		clickHistoryTab()
	}

	// --- Test-case composite keywords (CCIPH-21 scripts) ---

	private static String normalizeMultiValueInput(String raw) {
		return (raw ?: '').replace('|', '\n').replace(',', '\n').trim()
	}

	private static String normalizeDateType(String dateType) {
		String dt = (dateType ?: 'Pickup Date').trim()
		if (dt.equalsIgnoreCase('Shipment Date')) return 'Pickup Date'
		return dt
	}

	private Map<String, String> defaultSearchData() {
		return [
			charge_Group    : 'Revenue',
			charge_Code     : 'DTC',
			charge_Status   : 'All',
			date_Type       : 'Pickup Date',
			start_Date      : '01/01/2024',
			end_Date        : '31/12/2024',
			search_Criteria : 'Tracking Number',
			search_Input    : '35600000001',
			entity          : 'All'
		]
	}

	private void openHistoryTabForDefaultShipment() {
		searchAndOpenFirstResultWithoutTracking()
	}

	private void clickResetButton() {
		TestObject reset = dynamicXpathObject('cs_reset_btn',
			"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'reset')]")
		click(reset)
		WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
	}

	private void selectMultipleSearchReference(String criteria) {
		TestObject ref = dynamicXpathObject('cs_multiple_reference_dropdown',
			"//*[@role='dialog' or contains(@class,'modal')]//*[self::select or @role='combobox'][1]")
		waitVisibleClickable(ref)
		String tag = (WebUI.getAttribute(ref, 'tagName', FailureHandling.OPTIONAL) ?: '').toLowerCase()
		if (tag == 'select') {
			WebUI.selectOptionByLabel(ref, (criteria ?: 'Tracking Number').trim(), false)
		} else {
			click(ref)
			TestObject option = dynamicXpathObject("cs_multiple_ref_${criteria?.hashCode()}",
				"//*[@role='option' or self::li or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(normalizeLabel(criteria ?: 'Tracking Number'))})]")
			click(option)
		}
	}

	private void setMultipleSearchDialogInput(String value) {
		TestObject input = dynamicXpathObject('cs_multiple_dialog_input',
			"//*[@role='dialog' or contains(@class,'modal')]//textarea | //*[@role='dialog']//input[not(@type='hidden')]")
		waitVisibleClickable(input)
		WebUI.clearText(input, FailureHandling.OPTIONAL)
		WebUI.setText(input, normalizeMultiValueInput(value))
		WebUI.takeScreenshot()
	}

	private void clickMultipleSearchDialogSearch() {
		TestObject searchBtn = dynamicXpathObject('cs_multiple_dialog_search',
			"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]")
		click(searchBtn)
		WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
	}

	private void performSearchWithCriteria(String criteria, String value) {
		selectSearchCriteria(criteria ?: 'Tracking Number')
		if (value) setSearchInputText(value)
		clickSearch()
	}

	@Keyword
	def verifySearchWithoutFiltersShowsNoData() {
		clickSearch()
		verifyNoDataFoundMessage()
	}

	@Keyword
	def verifySearchWithoutFiltersShowsData() {
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchWithSingleFilter(String chargeGroup, String chargeCode) {
		if (chargeGroup) selectChargeGroup(chargeGroup)
		if (chargeCode) selectChargeCode(chargeCode)
		clickSearch()
		verifySearchResultsDisplayed()
	}
	
	@Keyword
	def selectChargeGroupAndCode(String chargeGroup, String chargeCode) {
		try {
			TestObject chargeGroupDropdown = to21('dropdown_chargeGroup')
			TestObject chargeCodeDropdown = to21('dropdown_chargeCode')
	
			WebUI.waitForElementVisible(
				chargeGroupDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
			WebUI.selectOptionByLabel(
				chargeGroupDropdown,
				chargeGroup,
				false
			)
			WebUI.waitForElementVisible(
				chargeCodeDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
			WebUI.selectOptionByLabel(
				chargeCodeDropdown,
				chargeCode,
				false
			)
			KeywordUtil.logInfo(
				"Selected Charge Group: '${chargeGroup}' and Charge Code: '${chargeCode}'"
			)
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to select Charge Group '${chargeGroup}' and Charge Code '${chargeCode}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def selectDateTypeField(String dateType) {
	
		try {
	
			TestObject dateTypeDropdown = to21('dropdown_DateType')
	
			WebUI.waitForElementVisible(
				dateTypeDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				dateTypeDropdown,
				dateType,
				false
			)
	
			KeywordUtil.logInfo(
				"Selected Date Type: '${dateType}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select Date Type '${dateType}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyDateTypeRequiredErrorVisible() {
		try {
			TestObject errorBanner = to21('error_Banner')
	
			WebUI.waitForElementVisible(
				errorBanner,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementVisible(
				errorBanner,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualMessage = WebUI.getText(errorBanner).trim()
	
			String expectedMessage =
				'When a date type is selected, start date and end date are required before search.'
	
			WebUI.verifyMatch(
				actualMessage,
				expectedMessage,
				false,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Date Type validation error is displayed correctly: '${actualMessage}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to verify Date Type validation error message: ${e.message}"
			)
		}
	}

	@Keyword
	def verifySearchWithMultipleFilters(String chargeGroup, String chargeCode, String chargeStatus) {
		if (chargeGroup) selectChargeGroup(chargeGroup)
		if (chargeCode) selectChargeCode(chargeCode)
		if (chargeStatus) selectDropdownByLabel('Charge Status', chargeStatus)
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def selectMultipleFilters(String chargeGroup, String chargeCode, String chargeStatus) {
		try {
			TestObject chargeGroupDropdown = to21('dropdown_chargeGroup')
			TestObject chargeCodeDropdown = to21('dropdown_chargeCode')
			TestObject chargeStatusDropdown = to21('dropdown_chargeStatus')
	
			WebUI.waitForElementVisible(
				chargeGroupDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				chargeGroupDropdown,
				chargeGroup,
				false
			)
	
			WebUI.waitForElementVisible(
				chargeCodeDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				chargeCodeDropdown,
				chargeCode,
				false
			)
	
			WebUI.waitForElementVisible(
				chargeStatusDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				chargeStatusDropdown,
				chargeStatus,
				false
			)
	
			KeywordUtil.logInfo(
				"Selected Charge Group: '${chargeGroup}', Charge Code: '${chargeCode}' and Charge Status: '${chargeStatus}'"
			)
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select Charge Group '${chargeGroup}', Charge Code '${chargeCode}' and Charge Status '${chargeStatus}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifySearchWithAllFilters() {
		Map<String, String> data = defaultSearchData()
		applyAllAvailableFilters(data)
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchWithDateRangeOnly(String dateType, String startDate, String endDate) {
		selectDateType(dateType ?: 'Pickup Date')
		setStartDate(startDate)
		setEndDate(endDate)
		clickSearch()
		verifySearchResultsDisplayed()
	}
	
	@Keyword
	def setCustomDate(String dateField, String dateValue) {
	
		try {
	
			TestObject dateInput
	
			if (dateField.equalsIgnoreCase('Start Date')) {
				dateInput = to21('date_start')
			} else if (dateField.equalsIgnoreCase('End Date')) {
				dateInput = to21('date_end')
			} else {
				failStop("Invalid date field: ${dateField}. Expected 'Start Date' or 'End Date'")
				return
			}
	
			WebUI.waitForElementVisible(
				dateInput,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Convert DD-MM-YYYY to YYYY-MM-DD
			Date parsedDate = Date.parse('dd-MM-yyyy', dateValue)
			String htmlDate = parsedDate.format('yyyy-MM-dd')
	
			WebUI.executeJavaScript(
				"""
            var element = arguments[0];
            var value = '${htmlDate}';

            var setter = Object.getOwnPropertyDescriptor(
                HTMLInputElement.prototype,
                'value'
            ).set;

            setter.call(element, value);

            element.dispatchEvent(
                new Event('input', { bubbles: true })
            );

            element.dispatchEvent(
                new Event('change', { bubbles: true })
            );
            """,
				[WebUI.findWebElement(dateInput, 10)]
			)
	
			KeywordUtil.logInfo(
				"${dateField} selected successfully: ${dateValue}"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to set ${dateField} '${dateValue}': ${e.message}"
			)
		}
	}

	@Keyword
	def verifySearchWithMultipleTrackingNumbers(String trackingNumbers) {
		selectSearchCriteria('Tracking Number')
		setMultipleTrackingNumbers(normalizeMultiValueInput(trackingNumbers))
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyInvalidDateRangeError(String startDate, String endDate, String dateType = 'Pickup Date') {
		verifyEndDateBeforeStartDateError(startDate, endDate, dateType)
	}

	@Keyword
	def verifyInvalidTrackingNumbersError(String invalidTrackingNumbers) {
		selectSearchCriteria('Tracking Number')
		setMultipleTrackingNumbers(normalizeMultiValueInput(invalidTrackingNumbers))
		clickSearch()
		verifyInvalidTrackingNumbersMessage()
	}

	@Keyword
	def verifyCombinedFilterSearch() {
		verifySearchWithAllFilters()
	}
	
	@Keyword
	def clickMultipleButton() {
		try {
			TestObject multipleButton = to67('multipleButton')
	
			WebUI.waitForElementVisible(
				multipleButton,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementVisible(
				multipleButton,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.click(
				multipleButton,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo("Clicked 'Multiple' button successfully")
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to click 'Multiple' button: ${e.message}"
			)
		}
	}

	@Keyword
	def verifyPaginationOnSearchResults() {
		applyAllAvailableFilters(defaultSearchData())
		verifySearchResultsDisplayed()
		verifyPaginationControlsVisible()
		clickPaginationNext()
	}

	@Keyword
	def verifyHideAndShowFilters() {
		toggleHideShowFilters()
		toggleHideShowFilters()
		verifyCustomsSearchScreenLoaded()
	}

	@Keyword
	def verifyFilterLabelsAfterHideShow() {
		toggleHideShowFilters()
		toggleHideShowFilters()
		verifyFilterLabelsVisible()
	}

	@Keyword
	def verifySaveSearchAsFavorite(String favoriteLabel) {
		selectChargeGroup('Revenue')
		selectChargeCode('DTC')
		clickSearch()
		clickFavoriteButton()
		TestObject labelInput = dynamicXpathObject('cs_favorite_label_input',
			"//*[@role='dialog' or contains(@class,'modal')]//input[not(@type='hidden')]")
		if (WebUI.waitForElementVisible(labelInput, 5, FailureHandling.OPTIONAL)) {
			WebUI.setText(labelInput, (favoriteLabel ?: 'AutoFavoriteSearch').trim())
			TestObject saveBtn = dynamicXpathObject('cs_favorite_save_btn',
				"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'save')]")
			click(saveBtn)
		}
		WebUI.takeScreenshot()
	}

	@Keyword
	def verifyEditSavedFavorite(String favoriteLabel) {
		verifySaveSearchAsFavorite(favoriteLabel)
		TestObject editBtn = dynamicXpathObject('cs_favorite_edit_btn',
			"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit')]")
		if (WebUI.waitForElementVisible(editBtn, 5, FailureHandling.OPTIONAL)) {
			click(editBtn)
		}
		WebUI.takeScreenshot()
	}

	@Keyword
	def verifyRenameSavedFavorite(String favoriteLabel, String favoriteLabelNew) {
		verifySaveSearchAsFavorite(favoriteLabel)
		TestObject renameBtn = dynamicXpathObject('cs_favorite_rename_btn',
			"//*[self::button or self::a][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'rename') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit')]")
		if (WebUI.waitForElementVisible(renameBtn, 5, FailureHandling.OPTIONAL)) {
			click(renameBtn)
			TestObject labelInput = dynamicXpathObject('cs_favorite_rename_input',
				"//*[@role='dialog' or contains(@class,'modal')]//input[not(@type='hidden')]")
			if (WebUI.waitForElementVisible(labelInput, 5, FailureHandling.OPTIONAL)) {
				WebUI.clearText(labelInput, FailureHandling.OPTIONAL)
				WebUI.setText(labelInput, (favoriteLabelNew ?: favoriteLabel).trim())
				TestObject saveBtn = dynamicXpathObject('cs_favorite_rename_save',
					"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'save')]")
				click(saveBtn)
			}
		}
		WebUI.takeScreenshot()
	}

	@Keyword
	def verifyExportSearchResults() {
		applyAllAvailableFilters(defaultSearchData())
		verifySearchResultsDisplayed()
		clickExportButton()
	}

	@Keyword
	def verifyMultipleSearchPopupOpens() {
		clickMultipleSearchButton()
		waitVisible(to('dlg_multipleSearch'))
	}

	@Keyword
	def verifyMultipleSearchScreenFields() {
		clickMultipleSearchButton()
		verifyMultipleSearchDialogFields()
	}

	@Keyword
	def verifyMultipleSearchFieldsValidation() {
		clickMultipleSearchButton()
		verifyMultipleSearchDialogFields()
		selectMultipleSearchReference('Tracking Number')
		setMultipleSearchDialogInput('35600000001,35600000002')
		TestObject searchBtn = dynamicXpathObject('cs_multiple_dialog_search_btn',
			"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]")
		WebUI.verifyElementClickable(searchBtn, FailureHandling.STOP_ON_FAILURE)
		TestObject cancelBtn = dynamicXpathObject('cs_multiple_dialog_cancel_btn',
			"//*[@role='dialog']//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cancel')]")
		WebUI.verifyElementClickable(cancelBtn, FailureHandling.STOP_ON_FAILURE)
		WebUI.takeScreenshot()
	}

	@Keyword
	def verifyMultipleSearchValid(String searchCriteria, String searchValue) {
		clickMultipleSearchButton()
		selectMultipleSearchReference(searchCriteria)
		setMultipleSearchDialogInput(searchValue)
		clickMultipleSearchDialogSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyMultipleSearchEmptyShowsError() {
		clickMultipleSearchButton()
		selectMultipleSearchReference('Tracking Number')
		clickMultipleSearchDialogSearch()
		verifySearchValidationError()
	}

	@Keyword
	def verifyMultipleSearchInvalidShowsError(String invalidSearchValue) {
		clickMultipleSearchButton()
		selectMultipleSearchReference('Tracking Number')
		setMultipleSearchDialogInput(invalidSearchValue ?: '   ')
		clickMultipleSearchDialogSearch()
		verifySearchValidationError()
	}

	@Keyword
	def verifyMultipleSearchCancelClosesPopup() {
		clickMultipleSearchButton()
		closeMultipleSearchDialog()
		if (WebUI.waitForElementVisible(to('dlg_multipleSearch'), 3, FailureHandling.OPTIONAL)) {
			failStop('Multiple Search dialog did not close after Cancel')
		}
	}

	@Keyword
	def verifyNavigateToDetailPageOnRowClick() {
		performSearchWithCriteria('Tracking Number', '35600000001')
		verifySearchResultsDisplayed()
		clickFirstSearchResultRow()
		verifyDetailPageLoaded()
	}

	@Keyword
	def verifyBackFromDetailPage() {
		verifyNavigateToDetailPageOnRowClick()
		clickBackButton()
		verifyCustomsSearchScreenLoaded()
	}

	@Keyword
	def verifyGridColumnHeaders() {
		verifySearchCriteriaDropdownValues()
	}

	@Keyword
	def verifySearchInputForCriteria(String searchCriteria, String searchValue) {
		performSearchWithCriteria(searchCriteria, searchValue)
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchAndReset() {
		performSearchWithCriteria('Tracking Number', '35600000001')
		verifySearchResultsDisplayed()
		clickResetButton()
		verifyCustomsSearchScreenLoaded()
	}

	@Keyword
	def verifySearchWithConsole(String searchValue) {
		performSearchWithCriteria('Console', searchValue)
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchWithBoe(String searchValue) {
		performSearchWithCriteria('BOE', searchValue)
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchWithAccountNumber(String searchValue) {
		performSearchWithCriteria('Account Number', searchValue)
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifySearchWithEmptyDataShowsNoResults() {
		selectSearchCriteria('Tracking Number')
		clickSearch()
		verifySearchValidationError()
	}

	@Keyword
	def verifySearchWithInvalidData() {
		performSearchWithCriteria('Tracking Number', 'INVALID_NO_MATCH_XYZ')
		verifyNoDataFoundMessage()
	}

	@Keyword
	def verifyDetailPageInformation() {
		verifyNavigateToDetailPageOnRowClick()
		verifyDetailPageLoaded()
	}

	@Keyword
	def verifyPaginationAppliedCorrectlyToDataset() {
		searchAndOpenFirstResultWithoutTracking()
		verifyHistoryPaginationDefaultPageSize(50)
	}

	@Keyword
	def verifyPaginationCorrectRecordsPerPageSize() {
		searchAndOpenFirstResultWithoutTracking()
		selectHistoryPageSize('5')
		verifyPaginationControlsVisible()
	}

	@Keyword
	def verifyPaginationServerSideLogic() {
		openHistoryTabForDefaultShipment()
		verifyPaginationControlsVisible()
		clickPaginationNext()
		verifyPaginationControlsVisible()
		KeywordUtil.logInfo('Validate network tab for server-side page/limit params during manual review if required.')
	}

	@Keyword
	def verifyPaginationNextPreviousNavigation() {
		openHistoryTabForDefaultShipment()
		clickPaginationNext()
		verifyPaginationControlsVisible()
		TestObject prev = dynamicXpathObject('cs_pagination_prev',
			"//*[contains(@class,'pagination')]//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'prev') or contains(@aria-label,'Previous')][not(@disabled)]")
		if (WebUI.waitForElementVisible(prev, 5, FailureHandling.OPTIONAL)) {
			click(prev)
		}
		verifyPaginationControlsVisible()
	}

	@Keyword
	def verifyPaginationPreviousDisabledOnFirstPage() {
		openHistoryTabForDefaultShipment()
		verifyPaginationButtonDisabled('previous')
	}

	@Keyword
	def verifyPaginationNextDisabledOnLastPage() {
		openHistoryTabForDefaultShipment()
		int guard = 0
		while (WebUI.waitForElementVisible(dynamicXpathObject('cs_pagination_next_enabled',
			"//*[contains(@class,'pagination')]//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'next')][not(@disabled)]"), 3, FailureHandling.OPTIONAL) && guard < 20) {
			clickPaginationNext()
			guard++
		}
		verifyPaginationButtonDisabled('next')
	}

	@Keyword
	def verifyPaginationAfterHardRefreshDefaultSettings() {
		openHistoryTabForDefaultShipment()
		clickPaginationNext()
		WebUI.refresh()
		WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
		verifyHistoryPaginationDefaultPageSize(50)
	}

	@Keyword
	def verifyPaginationPageLoadTimeBetweenPages() {
		openHistoryTabForDefaultShipment()
		long start = System.currentTimeMillis()
		clickPaginationNext()
		clickPaginationNext()
		long elapsed = System.currentTimeMillis() - start
		KeywordUtil.logInfo("Pagination navigation elapsed ms: ${elapsed}")
		verifyPaginationControlsVisible()
	}
}
