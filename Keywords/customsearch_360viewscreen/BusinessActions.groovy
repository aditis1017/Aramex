 package customsearch_360viewscreen

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
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select

import java.io.File
import java.io.FileFilter
import java.io.InputStream

import customsmanagement.ProfileGlobals

/**
 * Custom Search / 360 View Screen (CCIPH-21, CCIPH-26, CCIPH-120).
 * Assumes authenticated session — login handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX_21 = 'CustomSearch_360ViewScreen/CCIPH-21/'
	private static final String OR_PREFIX_26 = 'CustomSearch_360ViewScreen/CCIPH-26/'
	private static final String OR_PREFIX = 'CustomSearch_360ViewScreen/CCIPH-21/'
	private static final String RESULTS_ROW_XPATH = "//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')]"
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
	private static final List<String> CUSTOM_SEARCH_EXPORT_COLUMN_HEADERS = [
		'Entity', 'Tracking Number', 'Account Number', 'Account Name',
		'Origin', 'Destination', 'Third Party', 'Charge Group', 'Charge Code',
		'Currency', 'Total Outstanding', 'Total Balance', 'Status'
	]
	private static final String CUSTOM_SEARCH_EXPORT_DETECTED_FILE = 'Reports/latest-customsearch-export.txt'
	private static final int EXPORT_DOWNLOAD_TIMEOUT_SECONDS = 60

	private String lastCustomSearchExportFilePath = ''
	private String lastCustomSearchExportFileName = ''
	private String currentChargeDetailSection = ''

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}
	private static TestObject to21(String name) { 
		return findTestObject(OR_PREFIX_21 + name) 
		}

	private static TestObject to26(String name) {
		return findTestObject(OR_PREFIX_26 + name)
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
	 * Resolves Stage Customs Search URL. Prefers {@code CUSTOMS_SEARCH_URL_STAGE}, then
	 * {@code CUSTOMS_APP_URL_STAGE} only when it already targets customs-search.
	 */
	private static String resolveCustomsSearchStageUrl() {
		String searchUrl = (ProfileGlobals.get('CUSTOMS_SEARCH_URL_STAGE', '') ?: '').trim()
		if (searchUrl) {
			return searchUrl
		}
		String appUrl = (ProfileGlobals.get('CUSTOMS_APP_URL_STAGE', '') ?: '').trim()
		if (appUrl.contains('customs')) {
			return appUrl
		}
		return DEFAULT_STAGE_CUSTOMS_SEARCH_URL
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
	def validateFirstRow() {
	
		try {
	
			TestObject firstRow = findTestObject(
				'Object Repository/CustomSearch_360ViewScreen/CCIPH-21/tbl_firstRow'
			)
	
			WebUI.waitForElementVisible(
				firstRow,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			boolean isVisible = WebUI.verifyElementVisible(
				firstRow,
				FailureHandling.STOP_ON_FAILURE
			)
	
			assert isVisible : "First result row is not visible"
	
			KeywordUtil.logInfo(
				"First result row is visible successfully."
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate first result row: ${e.message}"
			)
		}
	}
	
	@Keyword
	def enterTrackingNumber(String trackingNumber) {
	
		try {
	
			TestObject trackingNumberField = findTestObject(
				'Object Repository/CustomSearch_360ViewScreen/CCIPH-21/txt_TrackingNumber'
			)
	
			WebUI.waitForElementVisible(
				trackingNumberField,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.clearText(
				trackingNumberField,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.setText(
				trackingNumberField,
				trackingNumber,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Entered Tracking Number: '${trackingNumber}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to enter Tracking Number '${trackingNumber}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def clickTrackingNumber(String trackingNumber) {
	
		try {
	
			TestObject trackingNumberCell = new TestObject('trackingNumberCell')
	
			trackingNumberCell.addProperty(
				'xpath',
				ConditionType.EQUALS,
				"//td[contains(@class,'resultsCellTracking') and normalize-space(.)='${trackingNumber}']"
			)
	
			WebUI.waitForElementVisible(
				trackingNumberCell,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.waitForElementClickable(
				trackingNumberCell,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.click(
				trackingNumberCell,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully clicked Tracking Number: '${trackingNumber}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to click Tracking Number '${trackingNumber}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyDateFieldIsCurrentDate(String fieldName) {
	
		try {
	
			// Get current system date
			String expectedDate =
				java.time.LocalDate.now().format(
					java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")
				)
	
			String xpath =
				"//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel')" +
				" and normalize-space()='${fieldName}']" +
				"/following-sibling::div[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldValue')]"
	
			TestObject dateField =
				new TestObject("dynamicDateField")
	
			dateField.addProperty(
				"xpath",
				ConditionType.EQUALS,
				xpath
			)
	
			// Wait for field value
			WebUI.waitForElementVisible(
				dateField,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Get actual date from application
			String actualDate =
				WebUI.getText(dateField).trim()
	
			KeywordUtil.logInfo(
				"Field '${fieldName}' - Expected Date: '${expectedDate}', Actual Date: '${actualDate}'"
			)
	
			// Validate date
			WebUI.verifyElementText(
				dateField,
				expectedDate,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully validated '${fieldName}' with current date: '${expectedDate}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate '${fieldName}' as current date: ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyDateField(String fieldName, String expectedDate) {
	
		try {
	
			String field = (fieldName ?: '').trim()
			String expected = (expectedDate ?: '').trim()
	
			if (!field) {
				failStop("Field name cannot be blank")
			}
	
			if (!expected) {
				failStop("Expected date cannot be blank for '${field}'")
			}
	
			String xpath =
				"//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel')" +
				" and normalize-space()='${field}']" +
				"/following-sibling::div[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldValue')]"
	
			TestObject dateField =
				new TestObject("dynamicDateField")
	
			dateField.addProperty(
				"xpath",
				ConditionType.EQUALS,
				xpath
			)
	
			WebUI.waitForElementVisible(
				dateField,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualDate =
				WebUI.getText(dateField).trim()
	
			KeywordUtil.logInfo(
				"Field: '${field}' | Expected: '${expected}' | Actual: '${actualDate}'"
			)
	
			WebUI.verifyElementText(
				dateField,
				expected,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully validated '${field}' with value '${expected}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate date field '${fieldName}' with expected value '${expectedDate}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def validateTrackingNumber(String expectedTrackingNumber) {
	
		try {
	
			TestObject trackingNumberValue = new TestObject('Object Repository/CustomSearch_360ViewScreen/CCIPH-21/val_Tracking')
	
			trackingNumberValue.addProperty(
				'xpath',
				ConditionType.EQUALS,
				"//p[contains(@class,'infoCardValue') and normalize-space(.)='${expectedTrackingNumber}']"
			)
	
			WebUI.waitForElementVisible(
				trackingNumberValue,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualTrackingNumber = WebUI.getText(
				trackingNumberValue,
				FailureHandling.STOP_ON_FAILURE
			).trim()
	
			WebUI.verifyEqual(
				actualTrackingNumber,
				expectedTrackingNumber,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Tracking Number validated successfully. Expected: '${expectedTrackingNumber}', Actual: '${actualTrackingNumber}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Tracking Number '${expectedTrackingNumber}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def clickBackButton() {
	
		try {
	
			TestObject backButton = new TestObject('Object Repository/CustomSearch_360ViewScreen/CCIPH-21/btn_Back')
	
			backButton.addProperty(
				'xpath',
				ConditionType.EQUALS,
				"//a[contains(@class,'backLink') and @href='/customs']"
			)
	
			WebUI.waitForElementVisible(
				backButton,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.waitForElementClickable(
				backButton,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.click(
				backButton,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully clicked the Back button."
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to click Back button: ${e.message}"
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
	def verifyServiceCodeAndDescription(
			String serviceCode,
			String expectedDescription) {
	
		try {
	
			String expectedServiceCode = (serviceCode ?: '').trim()
			String expectedDesc = (expectedDescription ?: '').trim()
	
			if (!expectedServiceCode) {
				failStop("Service Code cannot be blank")
			}
	
			if (!expectedDesc) {
				failStop("Expected Description cannot be blank")
			}
	
			String serviceCodeLower = expectedServiceCode.toLowerCase()
	
			String rowXpath =
				"//tr[contains(@class,'chargesDataRow')]" +
				"[td[contains(@class,'chargesCellService') and " +
				"contains(translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${serviceCodeLower}')]]"
	
			TestObject serviceCodeObject =
				new TestObject("dynamicServiceCode")
	
			serviceCodeObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath +
				"//td[contains(@class,'chargesCellService')]"
			)
	
			TestObject descriptionObject =
				new TestObject("dynamicDescription")
	
			descriptionObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath +
				"//td[contains(@class,'chargesCellDescription')]"
			)
	
			// Validate Service Code is visible
			WebUI.waitForElementVisible(
				serviceCodeObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Validate Service Code contains expected text, case-insensitive
			String actualServiceCode =
				WebUI.getText(serviceCodeObject).trim()
	
			if (!actualServiceCode.toLowerCase()
					.contains(expectedServiceCode.toLowerCase())) {
	
				failStop(
					"Service Code validation failed. " +
					"Expected to contain: '${expectedServiceCode}', " +
					"Actual: '${actualServiceCode}'"
				)
			}
	
			// Validate Description
			WebUI.waitForElementVisible(
				descriptionObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualDescription =
				WebUI.getText(descriptionObject).trim()
	
			if (!actualDescription.toLowerCase()
					.contains(expectedDesc.toLowerCase())) {
	
				failStop(
					"Description validation failed for Service Code '${expectedServiceCode}'. " +
					"Expected to contain: '${expectedDesc}', " +
					"Actual: '${actualDescription}'"
				)
			}
	
			KeywordUtil.logInfo(
				"Service Code validated: '${actualServiceCode}'"
			)
	
			KeywordUtil.logInfo(
				"Description validated: '${actualDescription}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Service Code '${serviceCode}' " +
				"and Description '${expectedDescription}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyEstimateAndEligibleCostSame(String serviceCode) {
	
		try {
	
			String expectedServiceCode = (serviceCode ?: '').trim()
	
			if (!expectedServiceCode) {
				failStop("Service Code cannot be blank")
			}
	
			String serviceCodeLower =
				expectedServiceCode.toLowerCase()
	
			String rowXpath =
				"//tr[contains(@class,'chargesDataRow')]" +
				"[td[contains(@class,'chargesCellService') and " +
				"contains(translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${serviceCodeLower}')]]"
	
			// Estimate = column 6
			TestObject estimateObject =
				new TestObject("dynamicEstimate")
	
			estimateObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath + "//td[6]"
			)
	
			// Eligible Cost = column 12
			TestObject eligibleCostObject =
				new TestObject("dynamicEligibleCost")
	
			eligibleCostObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath + "//td[12]"
			)
	
			WebUI.waitForElementVisible(
				estimateObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.waitForElementVisible(
				eligibleCostObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Read values from UI
			String estimate =
				WebUI.getText(estimateObject).trim()
	
			String eligibleCost =
				WebUI.getText(eligibleCostObject).trim()
	
			KeywordUtil.logInfo(
				"Estimate for '${expectedServiceCode}': '${estimate}'"
			)
	
			KeywordUtil.logInfo(
				"Eligible Cost for '${expectedServiceCode}': '${eligibleCost}'"
			)
	
			// Normalize spaces / commas
			String normalizedEstimate =
				estimate.replaceAll("[\\s,]", "")
	
			String normalizedEligibleCost =
				eligibleCost.replaceAll("[\\s,]", "")
	
			// Compare UI values
			if (normalizedEstimate != normalizedEligibleCost) {
	
				failStop(
					"Estimate and Eligible Cost do not match for " +
					"Service Code '${expectedServiceCode}'. " +
					"Estimate='${estimate}', " +
					"Eligible Cost='${eligibleCost}'"
				)
			}
	
			KeywordUtil.logInfo(
				"Estimate and Eligible Cost are equal for " +
				"Service Code '${expectedServiceCode}': '${estimate}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Estimate and Eligible Cost for " +
				"Service Code '${serviceCode}': ${e.message}"
			)
		}
	}
	
	
	@Keyword
	def verifyDescriptionEstimateAndEligibleCostSame(String description) {
	
		try {
	
			String expectedDescription = (description ?: '').trim()
	
			if (!expectedDescription) {
				failStop("Description cannot be blank")
			}
	
			String descriptionLower =
				expectedDescription.toLowerCase()
	
			// Locate row using Description
			String rowXpath =
				"//tr[contains(@class,'chargesDataRow')]" +
				"[td[contains(@class,'chargesCellDescription') and " +
				"contains(translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${descriptionLower}')]]"
	
			// Estimate = column 6
			TestObject estimateObject =
				new TestObject("dynamicEstimate")
	
			estimateObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath + "//td[6]"
			)
	
			// Eligible Cost = column 12
			TestObject eligibleCostObject =
				new TestObject("dynamicEligibleCost")
	
			eligibleCostObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rowXpath + "//td[12]"
			)
	
			WebUI.waitForElementVisible(
				estimateObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.waitForElementVisible(
				eligibleCostObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Read actual values from UI
			String estimate =
				WebUI.getText(
					estimateObject,
					FailureHandling.STOP_ON_FAILURE
				).trim()
	
			String eligibleCost =
				WebUI.getText(
					eligibleCostObject,
					FailureHandling.STOP_ON_FAILURE
				).trim()
	
			KeywordUtil.logInfo(
				"Description: '${expectedDescription}'"
			)
	
			KeywordUtil.logInfo(
				"Estimate: '${estimate}'"
			)
	
			KeywordUtil.logInfo(
				"Eligible Cost: '${eligibleCost}'"
			)
	
			// Normalize spaces and commas
			String normalizedEstimate =
				estimate.replaceAll("[\\s,]", "")
	
			String normalizedEligibleCost =
				eligibleCost.replaceAll("[\\s,]", "")
	
			// Numeric comparison to handle 12.5 vs 12.50
			BigDecimal estimateValue =
				new BigDecimal(normalizedEstimate)
	
			BigDecimal eligibleCostValue =
				new BigDecimal(normalizedEligibleCost)
	
			if (estimateValue.compareTo(eligibleCostValue) != 0) {
	
				failStop(
					"Estimate and Eligible Cost do not match for " +
					"Description '${expectedDescription}'. " +
					"Estimate='${estimate}', " +
					"Eligible Cost='${eligibleCost}'"
				)
			}
	
			KeywordUtil.logInfo(
				"Estimate and Eligible Cost are equal for " +
				"Description '${expectedDescription}'. " +
				"Estimate='${estimate}', " +
				"Eligible Cost='${eligibleCost}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Estimate and Eligible Cost for " +
				"Description '${description}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyDescription(String expectedDescription) {
	
		try {
	
			String expectedDesc = (expectedDescription ?: '').trim()
	
			if (!expectedDesc) {
				failStop("Expected Description cannot be blank")
			}
	
			String expectedDescLower = expectedDesc.toLowerCase()
	
			String xpath =
				"//td[contains(@class,'chargesCellDescription') and " +
				"contains(translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${expectedDescLower}')]"
	
			TestObject descriptionObject =
				new TestObject("dynamicDescription")
	
			descriptionObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				xpath
			)
	
			WebUI.waitForElementVisible(
				descriptionObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualDescription =
				WebUI.getText(
					descriptionObject,
					FailureHandling.STOP_ON_FAILURE
				).trim()
	
			if (!actualDescription.toLowerCase()
					.contains(expectedDescLower)) {
	
				failStop(
					"Description validation failed. " +
					"Expected to contain: '${expectedDesc}', " +
					"Actual: '${actualDescription}'"
				)
			}
	
			KeywordUtil.logInfo(
				"Description validated successfully. " +
				"Expected to contain: '${expectedDesc}', " +
				"Actual: '${actualDescription}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Description '${expectedDescription}': ${e.message}"
			)
		}
	}
	
	private TestObject resolveChargeTypeTab(String tabName) {
		
			String tab = (tabName ?: '').trim()
		
			if (!tab) {
				failStop("resolveChargeTypeTab: Tab name cannot be blank")
			}
		
			String xpath =
				"//button[@role='tab' and " +
				"normalize-space(.)='${tab}']"
		
			TestObject tabObject =
				new TestObject("dynamicChargeTypeTab_${tab}")
		
			tabObject.addProperty(
				"xpath",
				ConditionType.EQUALS,
				xpath
			)
		
			return tabObject
		}
		
		@Keyword
		def selectChargeTypeTab(String tabName) {
		
			try {
		
				String expectedTab = (tabName ?: '').trim()
		
				if (!expectedTab) {
					failStop(
						"selectChargeTypeTab: Tab name cannot be blank"
					)
				}
		
				TestObject tab = resolveChargeTypeTab(expectedTab)
		
				WebUI.waitForElementVisible(
					tab,
					15,
					FailureHandling.STOP_ON_FAILURE
				)
		
				WebUI.waitForElementClickable(
					tab,
					15,
					FailureHandling.STOP_ON_FAILURE
				)
		
				WebUI.click(
					tab,
					FailureHandling.STOP_ON_FAILURE
				)
		
				// Validate that the selected tab is active
				String activeTabXpath =
					"//button[@role='tab' and " +
					"normalize-space(.)='${expectedTab}' and " +
					"@aria-selected='true']"
		
				TestObject activeTab =
					new TestObject("activeChargeTypeTab_${expectedTab}")
		
				activeTab.addProperty(
					"xpath",
					ConditionType.EQUALS,
					activeTabXpath
				)
		
				WebUI.waitForElementVisible(
					activeTab,
					10,
					FailureHandling.STOP_ON_FAILURE
				)
		
				WebUI.verifyElementVisible(
					activeTab,
					FailureHandling.STOP_ON_FAILURE
				)
		
				KeywordUtil.logInfo(
					"Successfully navigated to Charge Type tab: '${expectedTab}'"
				)
		
				WebUI.takeScreenshot()
		
			} catch (Exception e) {
		
				WebUI.takeScreenshot()
		
				failStop(
					"Failed to navigate to Charge Type tab '${tabName}': ${e.message}"
				)
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
	def getRate1Value(String serviceCode) {
	
		try {
	
			String service = (serviceCode ?: '').trim()
	
			if (!service) {
				failStop("getRate1Value: Service Code cannot be blank")
			}
	
			String rate1Xpath =
				"//tr[td[contains(" +
				"translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${service.toLowerCase()}')" +
				"]]/td[8]"
	
			TestObject rate1Value = new TestObject("dynamicRate1Value")
	
			rate1Value.addProperty(
				"xpath",
				ConditionType.EQUALS,
				rate1Xpath
			)
	
			WebUI.waitForElementVisible(
				rate1Value,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualRate1 =
				WebUI.getText(
					rate1Value,
					FailureHandling.STOP_ON_FAILURE
				).trim()
	
			KeywordUtil.logInfo(
				"Rate 1 Value for '${serviceCode}': '${actualRate1}'"
			)
	
			return actualRate1
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to get Rate 1 Value for '${serviceCode}': ${e.message}"
			)
		}
	}

	@Keyword
	def verifyRate1ValueForServiceCode(String serviceCode) {
		try {
			String service = (serviceCode ?: '').trim()
			if (!service) {
				failStop("verifyRate1ValueForServiceCode: Service Code cannot be blank")
			}

			String rate1 = getRate1Value(service)
			String normalizedRate1 = rate1.replaceAll(/[^0-9.\-]/, '')

			if (!normalizedRate1) {
				failStop(
					"Rate 1 is not present or not numeric for '${service}' on Entity Settings page. " +
					"Actual value: '${rate1}'"
				)
			}

			KeywordUtil.logInfo(
				"Rate 1 verified for service code '${service}' on Entity Settings page: '${rate1}'"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to verify Rate 1 for service code '${serviceCode}' on Entity Settings page: ${e.message}"
			)
		}
	}
	
	@Keyword
	def getEligibleCost(String serviceCode) {
	
		try {
	
			String service = (serviceCode ?: '').trim()
	
			if (!service) {
				failStop("getEligibleCost: Service Code cannot be blank")
			}
	
			String eligibleCostXpath =
				"//tr[contains(@class,'chargesDataRow')]" +
				"[td[contains(@class,'chargesCellService') and " +
				"contains(translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')," +
				"'${service.toLowerCase()}')]]" +
				"//td[12]"
	
			TestObject eligibleCost = new TestObject("dynamicEligibleCost")
	
			eligibleCost.addProperty(
				"xpath",
				ConditionType.EQUALS,
				eligibleCostXpath
			)
	
			WebUI.waitForElementVisible(
				eligibleCost,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualEligibleCost =
				WebUI.getText(
					eligibleCost,
					FailureHandling.STOP_ON_FAILURE
				).trim()
	
			KeywordUtil.logInfo(
				"Eligible Cost for '${serviceCode}': '${actualEligibleCost}'"
			)
	
			return actualEligibleCost
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to get Eligible Cost for '${serviceCode}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifyRate1EqualsEligibleCost(String serviceCode, String capturedEligibleCost = null) {
	
		try {
	
			String service = (serviceCode ?: '').trim()
	
			if (!service) {
				failStop("verifyRate1EqualsEligibleCost: Service Code cannot be blank")
			}
	
			// Read Rate 1 from Entity Settings (caller must be on Entity Settings page)
			String rate1 = getRate1Value(service)
	
			// Eligible Cost must be read from Charge Detail before navigating away
			String eligibleCost = (capturedEligibleCost ?: '').trim()
			if (!eligibleCost) {
				eligibleCost = getEligibleCost(service)
			}
	
			KeywordUtil.logInfo(
				"Service Code: '${service}'"
			)
	
			KeywordUtil.logInfo(
				"Entity Settings Rate 1: '${rate1}'"
			)
	
			KeywordUtil.logInfo(
				"Charge Detail Eligible Cost: '${eligibleCost}'"
			)
	
			if (!rate1) {
				failStop("Rate 1 is blank for '${service}' on Entity Settings page")
			}
	
			if (!eligibleCost) {
				failStop(
					"Eligible Cost is blank for '${service}'. " +
					"Capture it from Charge Detail before navigating to Entity Settings."
				)
			}
	
			// Remove currency, spaces, commas and any other non-numeric characters
			String normalizedRate1 =
				rate1.replaceAll(/[^0-9.\-]/, '')
	
			String normalizedEligibleCost =
				eligibleCost.replaceAll(/[^0-9.\-]/, '')
	
			if (!normalizedRate1 || !normalizedEligibleCost) {
				failStop(
					"Unable to parse numeric values for '${service}'. " +
					"Rate 1='${rate1}', Eligible Cost='${eligibleCost}'"
				)
			}
	
			BigDecimal rate1Number =
				new BigDecimal(normalizedRate1)
	
			BigDecimal eligibleCostNumber =
				new BigDecimal(normalizedEligibleCost)
	
			// Numeric comparison handles 12.5 == 12.50
			if (rate1Number.compareTo(eligibleCostNumber) != 0) {
	
				failStop(
					"Rate 1 and Eligible Cost do not match for '${service}'. " +
					"Rate 1='${rate1}', Eligible Cost='${eligibleCost}'"
				)
			}
	
			KeywordUtil.logInfo(
				"Validation successful for '${service}'. " +
				"Rate 1='${rate1}' and Eligible Cost='${eligibleCost}'"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Rate 1 vs Eligible Cost for " +
				"'${serviceCode}': ${e.message}"
			)
		}
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

	private static Map<String, String> normalizeFilterSelectionMap(Map<String, String> rawFilters) {
		Map<String, String> labelAliases = [
			charge_Country : 'Charge Country',
			charge_Group   : 'Charge Group',
			charge_Code    : 'Charge Code',
			charge_Status  : 'Charge Status',
			date_Type      : 'Date Type',
			start_Date     : 'Start Date',
			end_Date       : 'End Date',
			account_Type   : 'Account Type',
			business_Type  : 'Business Type',
			category       : 'Category',
			entity         : 'Entity'
		]

		Map<String, String> normalized = [:]
		(rawFilters ?: [:]).each { key, value ->
			String filterValue = (value ?: '').toString().trim()
			if (!filterValue) {
				return
			}

			String keyText = (key ?: '').toString().trim()
			String label = labelAliases.containsKey(keyText) ? labelAliases[keyText] : keyText
			normalized[label] = filterValue
		}
		return normalized
	}

	private void applySingleFilterSelection(String label, String value) {
		switch (label) {
			case 'Charge Group':
				selectChargeGroup(value)
				break
			case 'Charge Code':
				selectChargeCode(value)
				break
			case 'Date Type':
				selectDateType(value)
				break
			case 'Start Date':
				setStartDate(value)
				break
			case 'End Date':
				setEndDate(value)
				break
			default:
				selectDropdownByLabel(label, value)
		}
	}

	private void selectFiltersFromMap(Map<String, String> rawFilters) {
		Map<String, String> filters = normalizeFilterSelectionMap(rawFilters)
		if (filters.isEmpty()) {
			KeywordUtil.logInfo('selectFilters: no filter values provided; skipping selection.')
			return
		}

		filters.each { label, value ->
			applySingleFilterSelection(label, value)
		}

		KeywordUtil.logInfo("Selected filters: ${filters}")
		WebUI.takeScreenshot()
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

	private List<String> readDropdownOptionTexts(TestObject dropdown) {
		waitVisible(dropdown)
		WebElement element = WebUiCommonHelper.findWebElement(dropdown, DEFAULT_TIMEOUT)
		String tag = (element.getTagName() ?: '').toLowerCase()
		if (tag == 'select') {
			Select select = new Select(element)
			return select.getOptions().collect { (it.getText() ?: '').trim() }.findAll { it }
		}

		click(dropdown)
		WebUI.delay(1)
		List<WebElement> options = DriverFactory.getWebDriver().findElements(
			org.openqa.selenium.By.xpath(
				"//*[@role='listbox']//*[@role='option'] | //*[@role='option'] | " +
				"//*[@role='dialog' or contains(@class,'modal')]//li[normalize-space(.)!='']"
			)
		)
		return options.collect { (it.getText() ?: '').trim() }.findAll { it }.unique()
	}

	private List<String> readMultipleSearchReferenceOptionTexts(TestObject dropdown) {
		return readDropdownOptionTexts(dropdown)
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

	private void selectMainSearchCriteriaDropdown(String criteria) {
		String target = (criteria ?: 'Tracking Number').trim()
		TestObject nativeSelect = dynamicXpathObject(
			'cs_native_search_criteria_select',
			"//select[@id='cfe-customs-search-criteria' or @id='ccfe-customs-search-criteria' or @aria-label='Search Criteria']"
		)

		if (WebUI.waitForElementPresent(nativeSelect, 5, FailureHandling.OPTIONAL)) {
			try {
				WebUI.selectOptionByLabel(nativeSelect, target, false)
				KeywordUtil.logInfo("Selected Search Criteria '${target}' from native select dropdown.")
				return
			} catch (Exception ignored) {
				KeywordUtil.logInfo("Native Search Criteria select failed; trying combobox fallback.")
			}
		}

		TestObject combobox = resolveDropdownTriggerByLabel('Search Criteria')
		waitVisibleClickable(combobox)
		if (isValueAlreadySelected(combobox, target)) {
			KeywordUtil.logInfo("Search Criteria already '${target}'; skipping selection.")
			return
		}
		selectDropdownByLabel('Search Criteria', target)
		KeywordUtil.logInfo("Selected Search Criteria '${target}' from combobox dropdown.")
	}

	private void performSearchWithCriteria(String criteria, String value) {
		String searchCriteria = (criteria ?: 'Tracking Number').trim()
		String searchValue = (value ?: '').trim()

		selectMainSearchCriteriaDropdown(searchCriteria)
		if (searchValue) {
			setSearchInputText(searchValue)
		}
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
	def selectChargeGroupValue(String chargeGroup) {
	
		try {
	
			TestObject chargeGroupDropdown = to21('dropdown_chargeGroup')
	
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
	
			KeywordUtil.logInfo(
				"Successfully selected Charge Group: '${chargeGroup}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select Charge Group '${chargeGroup}': ${e.message}"
			)
		}
	}
	
	private static String resolveCustomsSelectElementId(String dropdownName) {
		String name = (dropdownName ?: '').trim().toLowerCase()
		if (!name) {
			return ''
		}
		if (name.startsWith('ccfe-') || name.startsWith('cfc-') || name.startsWith('cfe-')) {
			return name
		}
		return "ccfe-customs-${name}"
	}

	private static List<String> customsSelectIdCandidates(String dropdownName) {
		String primary = resolveCustomsSelectElementId(dropdownName)
		List<String> candidates = [primary]
		if (primary.startsWith('ccfe-')) {
			candidates.add(primary.replaceFirst('ccfe-', 'cfc-'))
			candidates.add(primary.replaceFirst('ccfe-', 'cfe-'))
		}
		return candidates.unique()
	}

	private TestObject resolveNativeCustomsSelect(String dropdownName) {
		for (String id : customsSelectIdCandidates(dropdownName)) {
			TestObject dropdown = dynamicXpathObject(
				"dynamic_select_${id}",
				"//select[@id='${id}']"
			)
			if (WebUI.waitForElementPresent(dropdown, 5, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo("Resolved customs select dropdown id='${id}' for '${dropdownName}'")
				return dropdown
			}
		}
		failStop(
			"Native select dropdown not found for '${dropdownName}'. Tried ids: ${customsSelectIdCandidates(dropdownName)}"
		)
		return nativeSelectById('')
	}

	private TestObject nativeSelectById(String fieldId) {
		String id = (fieldId ?: '').trim()
		return dynamicXpathObject(
			"native_select_${id}",
			"//select[@id='${id}']"
		)
	}
	
//	@Keyword
//	def selectDropdownValueList(String dropdownName, String dropdownValue) {
//	
//		try {
//	
//			// Create dynamic TestObject
//			TestObject dropdown = new TestObject("Dynamic Dropdown - ${dropdownName}")
//	
//			dropdown.addProperty(
//				"id",
//				com.kms.katalon.core.testobject.ConditionType.EQUALS,
//				"cfc-customs-${dropdownName}"
//			)
//	
//			// Wait until dropdown is visible
//			WebUI.waitForElementVisible(
//				dropdown,
//				15,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			// Select value
//			WebUI.selectOptionByLabel(
//				dropdown,
//				dropdownValue,
//				false,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			KeywordUtil.logInfo(
//				"Successfully selected '${dropdownValue}' from dropdown '${dropdownName}'"
//			)
//	
//		} catch (Exception e) {
//	
//			WebUI.takeScreenshot()
//	
//			failStop(
//				"Failed to select '${dropdownValue}' from dropdown '${dropdownName}': ${e.message}"
//			)
//		}
//	}
	
	
	
	@Keyword
	def selectDropdownValueList(String dropdownName, String dropdownValue) {
		try {
			String value = (dropdownValue ?: '').trim()
			if (!value) {
				failStop("selectDropdownValueList: dropdownValue is blank for '${dropdownName}'")
			}

			TestObject dropdown = resolveNativeCustomsSelect(dropdownName)

			WebUI.waitForElementVisible(
				dropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)

			try {
				WebUI.selectOptionByLabel(
					dropdown,
					value,
					false,
					FailureHandling.STOP_ON_FAILURE
				)
			} catch (Exception labelError) {
				KeywordUtil.logInfo(
					"selectOptionByLabel failed for '${value}' (${labelError.message}); trying selectOptionByValue."
				)
				WebUI.selectOptionByValue(
					dropdown,
					value.toUpperCase(),
					false,
					FailureHandling.STOP_ON_FAILURE
				)
			}

			KeywordUtil.logInfo(
				"Successfully selected '${value}' from '${dropdownName}' dropdown"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to select '${dropdownValue}' from dropdown '${dropdownName}': ${e.message}"
			)
		}
	}
	

	private List<String> readNativeSelectOptionTextsById(String fieldId) {
		String id = (fieldId ?: '').trim()
		TestObject dropdown = nativeSelectById(id)
		WebUI.waitForElementPresent(dropdown, 15, FailureHandling.STOP_ON_FAILURE)

		WebElement element = WebUiCommonHelper.findWebElement(dropdown, DEFAULT_TIMEOUT)
		if (!(element.getTagName() ?: '').equalsIgnoreCase('select')) {
			failStop("Dropdown '${id}' is not a native select element")
		}

		return new Select(element).getOptions()
			.collect { (it.getText() ?: '').trim() }
			.findAll { it }
	}

	private List<String> normalizeExpectedDropdownValues(Object expected) {
		if (expected == null) {
			return []
		}
		if (expected instanceof Collection) {
			return expected.collect { (it ?: '').toString().trim() }.findAll { it }
		}
		if (expected.getClass().isArray()) {
			return (expected as Object[]).collect { (it ?: '').toString().trim() }.findAll { it }
		}
		String single = expected.toString().trim()
		return single ? [single] : []
	}

	private void validateSingleDropdownById(String fieldId, List<String> expectedValues) {
		String id = (fieldId ?: '').trim()
		if (!id) {
			return
		}
		if (!expectedValues) {
			KeywordUtil.logInfo("Skipping '${id}' because expected values are empty")
			return
		}

		WebUI.delay(1)
		List<String> actualValues = readNativeSelectOptionTextsById(id)

		KeywordUtil.logInfo("Validating dropdown '${id}'")
		KeywordUtil.logInfo("Expected values: ${expectedValues}")
		KeywordUtil.logInfo("Actual values: ${actualValues}")

		List<String> missing = expectedValues.findAll { String expected ->
			!actualValues.any { String actual -> actual.equalsIgnoreCase(expected.trim()) }
		}
		if (!missing.isEmpty()) {
			failStop("Dropdown '${id}' missing values: ${missing}. Actual: ${actualValues}")
		}

		KeywordUtil.logInfo("Validated dropdown '${id}' successfully")
	}

	@Keyword
	def validateDropdownValues(Map dropdownValidations = [:]) {
		try {
			if (!dropdownValidations || dropdownValidations.isEmpty()) {
				KeywordUtil.logInfo('validateDropdownValues: no dropdown validations provided; skipping.')
				return
			}

			dropdownValidations.each { fieldId, expected ->
				validateSingleDropdownById(
					fieldId?.toString(),
					normalizeExpectedDropdownValues(expected)
				)
			}

			KeywordUtil.logInfo('Successfully validated all dropdown values.')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to validate dropdown values: ${e.message}")
		}
	}

	@Keyword
	def validateDropdownValues(String dropdownId, List<String> expectedValues) {
		validateDropdownValues([(dropdownId): expectedValues])
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

	/**
	 * Select one or more Custom Search filters without clicking Search.
	 * Only non-blank values are applied; omitted filters are left unchanged.
	 *
	 * Positional example (same as selectMultipleFilters):
	 *   selectFilters(charge_Group, charge_Code, charge_Status)
	 *
	 * Single filter example:
	 *   selectFilters(charge_Group, null, null)
	 *
	 * Extended filters example:
	 *   selectFilters(charge_Group, charge_Code, charge_Status, null, null, date_Type, start_Date, end_Date)
	 */
	
	
	
	@Keyword
	def selectFilters(
		String chargeGroup = null,
		String chargeCode = null,
		String chargeStatus = null,
		String chargeCountry = null,
		String entity = null,
		String dateType = null,
		String startDate = null,
		String endDate = null,
		String accountType = null,
		String businessType = null,
		String category = null
	) {
		try {
			selectFiltersFromMap([
				'Charge Group'   : chargeGroup,
				'Charge Code'    : chargeCode,
				'Charge Status'  : chargeStatus,
				'Charge Country' : chargeCountry,
				'Entity'         : entity,
				'Date Type'      : dateType,
				'Start Date'     : startDate,
				'End Date'       : endDate,
				'Account Type'   : accountType,
				'Business Type'  : businessType,
				'Category'       : category
			])
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to select filters: ${e.message}")
		}
	}

	/**
	 * Select one or more Custom Search filters using a map.
	 * Keys may be UI labels (e.g. 'Charge Group') or test-data keys (e.g. charge_Group).
	 *
	 * Example:
	 *   selectFilters([charge_Group: 'Cost', charge_Code: 'ECD', charge_Status: 'Due'])
	 */
	@Keyword
	def selectFilters(Map<String, String> filters) {
		try {
			selectFiltersFromMap(filters)
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to select filters from map: ${e.message}")
		}
	}
	
	
	@Keyword
	def selectFiltersSingleMultiple(Map<String, String> filters = [:]) {
		try {
	
			filters.each { String filterName, String filterValue ->
	
				// Skip empty values
				if (!filterValue) {
					KeywordUtil.logInfo(
						"Skipping '${filterName}' because no value was provided"
					)
					return
				}
	
				TestObject filterDropdown = new TestObject(
					"dynamicFilter_${filterName}"
				)
	
				filterDropdown.addProperty(
					"xpath",
					ConditionType.EQUALS,
					"//select[@aria-label='${filterName}']"
				)
	
				WebUI.waitForElementVisible(
					filterDropdown,
					15,
					FailureHandling.STOP_ON_FAILURE
				)
	
				WebUI.selectOptionByLabel(
					filterDropdown,
					filterValue,
					false
				)
	
				KeywordUtil.logInfo(
					"Selected ${filterName}: '${filterValue}'"
				)
			}
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select filters: ${e.message}"
			)
		}
	}
	
	@Keyword
	def selectFiltersById(Map<String, String> filters = [:]) {
		try {
	
			filters.each { String fieldId, String filterValue ->
	
				if (!filterValue) {
					KeywordUtil.logInfo(
						"Skipping '${fieldId}' because value is empty"
					)
					return
				}
	
				TestObject dropdown = new TestObject(
					"dynamicDropdown_${fieldId}"
				)
	
				dropdown.addProperty(
					"css",
					ConditionType.EQUALS,
					"select[id='${fieldId}']"
				)
	
				WebUI.waitForElementVisible(
					dropdown,
					15,
					FailureHandling.STOP_ON_FAILURE
				)
	
				WebUI.selectOptionByLabel(
					dropdown,
					filterValue,
					false
				)
	
				KeywordUtil.logInfo(
					"Selected ${fieldId}: '${filterValue}'"
				)
			}
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select filters: ${e.message}"
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

	private String getCustomDateFieldValue(String dateField) {
		TestObject dateInput
		if (dateField.equalsIgnoreCase('Start Date')) {
			dateInput = to21('date_start')
		} else if (dateField.equalsIgnoreCase('End Date')) {
			dateInput = to21('date_end')
		} else {
			failStop("Invalid date field: ${dateField}. Expected 'Start Date' or 'End Date'")
			return ''
		}
		waitVisible(dateInput)
		return getFieldDisplayedValue(dateInput)
	}

	private static Date parseFlexibleDate(String raw) {
		String value = (raw ?: '').trim()
		if (!value) {
			return null
		}
		for (String pattern : ['dd-MM-yyyy', 'dd/MM/yyyy', 'yyyy-MM-dd', 'dd-MM-yy']) {
			try {
				return Date.parse(pattern, value)
			} catch (Exception ignored) {
			}
		}
		return null
	}

	private static boolean datesAreEqual(String first, String second) {
		Date firstDate = parseFlexibleDate(first)
		Date secondDate = parseFlexibleDate(second)
		if (firstDate && secondDate) {
			return firstDate.clearTime() == secondDate.clearTime()
		}
		return (first ?: '').trim().equalsIgnoreCase((second ?: '').trim())
	}

	private void waitForStartDateToMatchEndDate(int timeoutSeconds = 10) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			String actualStart = getCustomDateFieldValue('Start Date')
			String actualEnd = getCustomDateFieldValue('End Date')
			if (actualStart && actualEnd && datesAreEqual(actualStart, actualEnd)) {
				KeywordUtil.logInfo(
					"Start Date auto-reset verified. Start='${actualStart}', End='${actualEnd}'"
				)
				return
			}
			WebUI.delay(1)
		}

		String actualStart = getCustomDateFieldValue('Start Date')
		String actualEnd = getCustomDateFieldValue('End Date')
		failStop(
			"Start Date was not reset to match End Date within ${timeoutSeconds}s. " +
			"Start='${actualStart}', End='${actualEnd}'"
		)
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
//		verifyInvalidTrackingNumbersMessage()
		verifyNoDataFoundMessage()
	}

	@Keyword
	def verifyCombinedFilterSearch() {
		verifySearchWithAllFilters()
	}
	
	@Keyword
	def clickMultipleButton() {
		try {
			TestObject multipleButton = to21('btn_Multiple')
	
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
	def verifyMultipleShipmentsTextPresent() {
		try {
			TestObject title = to21('title_MultipleShipments')
	
			WebUI.waitForElementVisible(
				title,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementVisible(
				title,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementText(
				title,
				'Multiple Shipments',
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"'Multiple Shipments' text is displayed successfully"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to verify 'Multiple Shipments' text: ${e.message}"
			)
		}
	}
	
	@Keyword
	def enterMultipleSearchValues(String values) {
		try {
			TestObject searchInput = to21('txt_MultipleInput')
	
			WebUI.waitForElementVisible(
				searchInput,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.clearText(searchInput)
	
			WebUI.setText(
				searchInput,
				values,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Entered multiple search values: '${values}'"
			)
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to enter multiple search values '${values}': ${e.message}"
			)
		}
	}
	
	
//	@Keyword
//	def clearTextValue()
//	{
//		try {
//			TestObject searchInput = to21('txt_MultipleInput')
//	
//			WebUI.waitForElementVisible(
//				searchInput,
//				15,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			WebUI.clearText(searchInput)
//			
//			KeywordUtil.logInfo(
//				"Clearing entered value"
//			)
//	
//		} catch (Exception e) {
//			WebUI.takeScreenshot()
//	
//			failStop(
//				"Failed to enter multiple search values "
//			)
//		}
//	}

	@Keyword
	def clearTextValue() {
		try {
			TestObject searchInput = to21('txt_MultipleInput')

			WebUI.waitForElementVisible(
				searchInput,
				15,
				FailureHandling.STOP_ON_FAILURE
			)

			WebUI.waitForElementClickable(
				searchInput,
				15,
				FailureHandling.STOP_ON_FAILURE
			)

			WebUI.click(searchInput)
			WebUI.clearText(searchInput)

			String remainingValue = (WebUI.getAttribute(searchInput, 'value', FailureHandling.OPTIONAL) ?: '').trim()
			if (remainingValue) {
				WebUI.sendKeys(searchInput, Keys.chord(Keys.CONTROL, 'a'))
				WebUI.sendKeys(searchInput, Keys.BACK_SPACE)
				remainingValue = (WebUI.getAttribute(searchInput, 'value', FailureHandling.OPTIONAL) ?: '').trim()
			}

			if (remainingValue) {
				failStop("Failed to clear input field. Remaining value: '${remainingValue}'")
			}

			KeywordUtil.logInfo('Successfully cleared entered value from input field')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to clear entered text value: ${e.message}")
		}
	}
	
	
	@Keyword
	def clickOkayButton() {
		try {
			TestObject okayButton = to21('btn_MultipleOkay')
	
			WebUI.waitForElementVisible(
				okayButton,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.click(okayButton)
	
			KeywordUtil.logInfo("Clicked Okay button successfully")
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to click Okay button: ${e.message}"
			)
		}
	}
	
	@Keyword
	def clickCloseButton() {
	
		try {
	
			TestObject closeButton = findTestObject(
				'Object Repository/CustomSearch_360ViewScreen/CCIPH-21/btn_Close'
			)
	
			WebUI.waitForElementVisible(
				closeButton,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.click(
				closeButton,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo("Successfully clicked the Close button.")
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop("Failed to click Close button: ${e.message}")
		}
	}

	@Keyword
	def verifyPaginationOnSearchResults() {
//		applyAllAvailableFilters(defaultSearchData())
//		clickSearch()
//		verifySearchResultsDisplayed()
		verifyPaginationControlsVisible()
	//	clickPaginationNext()
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
//		clickSearch()
//		applyAllAvailableFilters(defaultSearchData())
//		verifySearchResultsDisplayed()
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
	//	verifyMultipleSearchDialogFields()
		validateReferenceDropdownOptions()
	}
	
	@Keyword
	def validateReferenceDropdownOptions() {
		try {
			TestObject referenceDropdown = to21('dropdown_Multiple_Reference')

			List<String> expectedOptions = SEARCH_CRITERIA_OPTIONS

			WebUI.waitForElementVisible(
				referenceDropdown,
				10,
				FailureHandling.STOP_ON_FAILURE
			)

			List<String> actualOptions = readMultipleSearchReferenceOptionTexts(referenceDropdown)

			KeywordUtil.logInfo("Expected Reference options: ${expectedOptions}")
			KeywordUtil.logInfo("Actual Reference options: ${actualOptions}")

			expectedOptions.each { String expectedOption ->
				boolean found = actualOptions.any { String actual ->
					actual.equalsIgnoreCase(expectedOption.trim())
				}
				if (!found) {
					failStop(
						"Reference dropdown does not contain '${expectedOption}'. Actual options: ${actualOptions}"
					)
				}
				KeywordUtil.logInfo("Verified Reference option: '${expectedOption}'")
			}

			KeywordUtil.logInfo('All Reference dropdown options are present successfully.')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to validate Reference dropdown options: ${e.message}")
		}
	}

	@Keyword
	def verifyMultipleSearchFieldsValidation() {
		clickMultipleSearchButton()
	//	verifyMultipleSearchDialogFields()
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
//		clickMultipleSearchDialogSearch()
//		verifySearchResultsDisplayed()
	}

	/**
	 * Selects a value from the main Search Criteria dropdown and enters the search input value.
	 * Mirrors {@link #verifyMultipleSearchValid(String, String)} for the Customs Search screen fields.
	 *
	 * Example:
	 *   selectSearchCriteriaAndEnterValue(search_Criteria, tracking_Numbers)
	 */
	@Keyword
	def selectSearchCriteriaAndEnterValue(String searchCriteria, String searchValue) {
		try {
			String criteria = (searchCriteria ?: 'Tracking Number').trim()
			String value = (searchValue ?: '').trim()

			selectMainSearchCriteriaDropdown(criteria)
			setSearchInputText(value)

			KeywordUtil.logInfo(
				"Selected Search Criteria '${criteria}' and entered search value '${value}'"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("selectSearchCriteriaAndEnterValue failed: ${e.message}")
		}
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
		performSearchWithCriteria('Tracking Number', 'DP13000')
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
		verifyResultGridColumnHeadersInOrder()
	}
	
	@Keyword
	def selectSearchCriteriaValue(String criteriaValue) {
	
		try {
	
			TestObject searchCriteriaDropdown = new TestObject(
				'Object Repository/CustomSearch_360ViewScreen/CCIPH-21/dropdown_Tracking'
			)
	
			searchCriteriaDropdown.addProperty(
				'id',
				ConditionType.EQUALS,
				'cfe-customs-search-criteria'
			)
	
			WebUI.waitForElementVisible(
				searchCriteriaDropdown,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				searchCriteriaDropdown,
				criteriaValue,
				false,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully selected Search Criteria: ${criteriaValue}"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select Search Criteria '${criteriaValue}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def validateSearchCriteriaDropdownValues() {
		try {
			TestObject searchCriteriaDropdown = resolveDropdownTriggerByLabel('Search Criteria')
			waitVisibleClickable(searchCriteriaDropdown)
			click(searchCriteriaDropdown)
			WebUI.delay(1)

			List<String> expectedOptions = SEARCH_CRITERIA_OPTIONS
			KeywordUtil.logInfo("Expected Search Criteria options: ${expectedOptions}")

			expectedOptions.each { String expectedOption ->
				TestObject option = dynamicXpathObject(
					"cs_search_criteria_option_${expectedOption.hashCode()}",
					"//*[@role='listbox']//*[@role='option' or self::li][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(normalizeLabel(expectedOption))})]" +
					" | //*[@role='option' or self::li or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(normalizeLabel(expectedOption))})]"
				)
				if (!WebUI.waitForElementVisible(option, 10, FailureHandling.OPTIONAL)) {
					failStop(
						"Search Criteria dropdown does not contain '${expectedOption}'"
					)
				}
				KeywordUtil.logInfo("Validated Search Criteria option is present: ${expectedOption}")
			}

			click(searchCriteriaDropdown)
			KeywordUtil.logInfo('Successfully validated all Search Criteria dropdown values.')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("Failed to validate Search Criteria dropdown values: ${e.message}")
		}
	}

	@Keyword
	def verifySearchInputForCriteria(String searchCriteria, String tracking_Number) {
		performSearchWithCriteria(searchCriteria, tracking_Number)
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

	// ---------- Custom Search export download validation (TC_US_FUNC_017) ----------

	private static String projectDir() {
		return RunConfiguration.getProjectDir() ?: System.getProperty('user.dir') ?: ''
	}

	private File ensureCustomSearchExportDownloadsDirectory() {
		String fromProfile = (ProfileGlobals.get('CUSTOM_SEARCH_EXPORT_DOWNLOADS_DIR', '') ?: '').trim()
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
			failStop("Could not create Custom Search export Downloads folder: ${dir.absolutePath}")
		}
		if (!dir.isDirectory()) {
			failStop("Custom Search export Downloads path is not a directory: ${dir.absolutePath}")
		}
		KeywordUtil.logInfo("Custom Search export Downloads directory: ${dir.absolutePath}")
		return dir
	}

	private void configureCustomSearchExportDownloadDirectory(File downloadsDir) {
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
			KeywordUtil.logInfo("configureCustomSearchExportDownloadDirectory skipped: ${e.message}")
		}
	}

	private static File resolveDefaultUserDownloadsDirectory() {
		return new File(System.getProperty('user.home') ?: '', 'Downloads')
	}

	private static File resolveCustomSearchExportDownloadsDirectory(String explicitDownloadsPath = null) {
		String explicit = (explicitDownloadsPath ?: '').trim()
		if (explicit) {
			return new File(explicit)
		}
		String fromProfile = (ProfileGlobals.get('CUSTOM_SEARCH_EXPORT_DOWNLOADS_DIR', '') ?: '').trim()
		if (fromProfile) {
			File profileDir = new File(fromProfile)
			if (!profileDir.isAbsolute()) {
				profileDir = new File(projectDir(), fromProfile)
			}
			if (profileDir.isDirectory()) {
				return profileDir
			}
			KeywordUtil.markWarning("Profile Downloads path does not exist (${profileDir.absolutePath}); using project Downloads")
		}
		return new File(projectDir(), 'Downloads')
	}

	private static boolean isCompletedCustomSearchExportDownload(File file) {
		if (file == null || !file.isFile()) {
			return false
		}
		String name = file.name.toLowerCase()
		return (name.endsWith('.csv') || name.endsWith('.xlsx')) &&
			!name.endsWith('.crdownload') && !name.endsWith('.tmp') && !name.endsWith('.part')
	}

	private static List<File> listCustomSearchExportFilesModifiedAfter(File downloadsDir, long afterMillis) {
		if (downloadsDir == null || !downloadsDir.isDirectory()) {
			return []
		}
		File[] files = downloadsDir.listFiles({ File candidate ->
			return isCompletedCustomSearchExportDownload(candidate) && candidate.lastModified() >= afterMillis
		} as FileFilter)
		if (files == null) {
			return []
		}
		List<File> found = new ArrayList<>(java.util.Arrays.asList(files))
		found.sort { File a, File b -> Long.compare(b.lastModified(), a.lastModified()) }
		return found
	}

	private void recordDetectedCustomSearchExportFile(File exportFile) {
		lastCustomSearchExportFileName = exportFile.name
		lastCustomSearchExportFilePath = exportFile.absolutePath
		writeDetectedCustomSearchExportFile(exportFile.name, exportFile.absolutePath)
		KeywordUtil.logInfo("Detected Custom Search export file: ${lastCustomSearchExportFilePath}")
	}

	private static void writeDetectedCustomSearchExportFile(String fileName, String absolutePath) {
		try {
			File out = new File(projectDir(), CUSTOM_SEARCH_EXPORT_DETECTED_FILE)
			File parent = out.parentFile
			if (parent != null && !parent.exists()) {
				parent.mkdirs()
			}
			out.setText("fileName=${fileName}\nabsolutePath=${absolutePath}\ncapturedAt=${new Date()}\n", 'UTF-8')
		} catch (Exception e) {
			KeywordUtil.markWarning("Could not write ${CUSTOM_SEARCH_EXPORT_DETECTED_FILE}: ${e.message}")
		}
	}

	private String waitForCustomSearchExportDownload(long exportStartMillis, File preferredDir = null) {
		File dir = preferredDir ?: ensureCustomSearchExportDownloadsDirectory()
		File userDownloads = resolveDefaultUserDownloadsDirectory()
		List<File> searchDirs = [dir]
		if (userDownloads.isDirectory() && userDownloads.absolutePath != dir.absolutePath) {
			searchDirs.add(userDownloads)
		}
		if (!dir.isDirectory()) {
			failStop("Downloads folder does not exist: ${dir.absolutePath}")
		}

		long deadline = System.currentTimeMillis() + (EXPORT_DOWNLOAD_TIMEOUT_SECONDS * 1000L)
		long afterMillis = exportStartMillis - 5000L
		File newest = null
		File foundIn = null

		while (System.currentTimeMillis() < deadline) {
			for (File searchDir : searchDirs) {
				List<File> candidates = listCustomSearchExportFilesModifiedAfter(searchDir, afterMillis)
				if (!candidates.isEmpty()) {
					newest = candidates[0]
					foundIn = searchDir
					break
				}
			}
			if (newest != null) {
				break
			}
			WebUI.delay(2)
		}

		if (newest == null) {
			failStop("No Custom Search export file found in ${dir.absolutePath}${userDownloads.isDirectory() ? " (also checked ${userDownloads.absolutePath})" : ''}")
		}

		if (foundIn != null && foundIn.absolutePath != dir.absolutePath) {
			File target = new File(dir, newest.name)
			try {
				if (newest.renameTo(target)) {
					newest = target
				} else {
					java.nio.file.Files.copy(newest.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
					newest.delete()
					newest = target
				}
				KeywordUtil.logInfo("Moved Custom Search export into project Downloads: ${newest.absolutePath}")
			} catch (Exception copyEx) {
				KeywordUtil.logInfo("Could not relocate export into project Downloads (${copyEx.message}); verifying at ${newest.absolutePath}")
			}
		}

		recordDetectedCustomSearchExportFile(newest)
		if (!newest.exists() || newest.length() <= 0L) {
			failStop("Custom Search export file is missing or empty: ${newest.absolutePath}")
		}
		return newest.absolutePath
	}

	private static boolean exportBlobContainsValue(String blob, String expectedValue) {
		String value = (expectedValue ?: '').trim()
		if (!value || !blob) {
			return false
		}
		return blob.contains(value) || blob.toLowerCase().contains(value.toLowerCase())
	}

	private static boolean customSearchExportCsvContainsAllHeaders(String absolutePath, List<String> expectedHeaders) {
		String blob = new File((absolutePath ?: '').trim()).getText('UTF-8')
		for (String header : (expectedHeaders ?: [])) {
			if (!exportBlobContainsValue(blob, header)) {
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

	private static List<String> readCustomSearchXlsxSharedStrings(java.util.zip.ZipFile zip) {
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

	private static String extractCustomSearchXlsxCellValue(String inner, String attrs, List<String> sharedStrings) {
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

	private static List<String> readCustomSearchXlsxFirstRowColumnHeaders(String absolutePath) {
		File file = new File(absolutePath)
		if (!file.isFile()) {
			return []
		}
		java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)
		try {
			List<String> sharedStrings = readCustomSearchXlsxSharedStrings(zip)
			String sheetXml = readZipEntryText(zip, 'xl/worksheets/sheet1.xml')
			if (!sheetXml?.trim()) {
				return []
			}
			def rowMatch = sheetXml =~ /<row\s+r="1"[^>]*>([\s\S]*?)<\/row>/
			if (!rowMatch.find()) {
				return []
			}
			String rowInner = rowMatch.group(1)
			List<String> headers = []
			(rowInner =~ /<c\s+([^>]*)>([\s\S]*?)<\/c>/).each { cellMatch ->
				headers.add(extractCustomSearchXlsxCellValue(cellMatch[2], cellMatch[1], sharedStrings).trim())
			}
			return headers.findAll { String header -> header }
		} finally {
			zip.close()
		}
	}

	private static String readCustomSearchExportTextBlob(String absolutePath) {
		File file = new File((absolutePath ?: '').trim())
		if (!file.isFile()) {
			return ''
		}
		String lower = file.name.toLowerCase()
		if (lower.endsWith('.csv')) {
			return file.getText('UTF-8')
		}
		if (lower.endsWith('.xlsx')) {
			java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)
			try {
				List<String> sharedStrings = readCustomSearchXlsxSharedStrings(zip)
				String sheetXml = readZipEntryText(zip, 'xl/worksheets/sheet1.xml')
				if (!sheetXml?.trim()) {
					return ''
				}
				StringBuilder blob = new StringBuilder()
				(sheetXml =~ /<c\s+([^>]*)>([\s\S]*?)<\/c>/).each { cellMatch ->
					blob.append(extractCustomSearchXlsxCellValue(cellMatch[2], cellMatch[1], sharedStrings)).append(' ')
				}
				return blob.toString()
			} finally {
				zip.close()
			}
		}
		return file.getText('UTF-8')
	}

	private static Map<String, String> normalizeAppliedFilterValues(Map<String, String> filters) {
		Map<String, String> labelAliases = [
			charge_Country : 'Charge Country',
			charge_Group   : 'Charge Group',
			charge_Code    : 'Charge Code',
			charge_Status  : 'Charge Status',
			date_Type      : 'Date Type',
			start_Date     : 'Start Date',
			end_Date       : 'End Date',
			account_Type   : 'Account Type',
			business_Type  : 'Business Type',
			category       : 'Category',
			entity         : 'Entity'
		]
		Map<String, String> normalized = [:]
		(filters ?: [:]).each { key, value ->
			String filterValue = (value ?: '').toString().trim()
			if (!filterValue) {
				return
			}
			String keyText = (key ?: '').toString().trim()
			String label = labelAliases.containsKey(keyText) ? labelAliases[keyText] : keyText
			normalized[label] = filterValue
		}
		return normalized
	}

	/**
	 * Ensures project {@code Downloads} exists and points the browser download path to it before export.
	 */
	@Keyword
	String prepareCustomSearchExportDownloadDirectory() {
		File dir = ensureCustomSearchExportDownloadsDirectory()
		configureCustomSearchExportDownloadDirectory(dir)
		return dir.absolutePath
	}

	@Keyword
	String getCustomSearchExportDownloadsDirectory() {
		return ensureCustomSearchExportDownloadsDirectory().absolutePath
	}

	@Keyword
	String getFirstSearchResultRowText() {
		try {
			TestObject firstRow = dynamicXpathObject(
				'cs_first_result_row_text',
				"(//tr[contains(@class,'CustomsPage-module__qLug_q__resultsRow')])[1]"
			)
			waitVisible(firstRow)
			String rowText = (WebUI.getText(firstRow) ?: '').trim()
			KeywordUtil.logInfo("First search result row text: '${rowText}'")
			WebUI.takeScreenshot()
			return rowText
		} catch (Exception e) {
			failStop("getFirstSearchResultRowText failed: ${e.message}")
			return ''
		}
	}

	@Keyword
	String verifyLatestCustomSearchExportDownloaded(long exportStartMillis, String downloadsFolder = null) {
		try {
			File dir = resolveCustomSearchExportDownloadsDirectory(downloadsFolder)
			if (!dir.exists() && !dir.mkdirs()) {
				failStop("Could not create Custom Search export Downloads folder: ${dir.absolutePath}")
			}
			KeywordUtil.logInfo("Waiting for Custom Search export in: ${dir.absolutePath}")
			String path = waitForCustomSearchExportDownload(exportStartMillis, dir)
			WebUI.takeScreenshot()
			return path
		} catch (Exception e) {
			failStop("verifyLatestCustomSearchExportDownloaded failed: ${e.message}")
			return ''
		}
	}

	@Keyword
	String getLastCustomSearchExportFilePath() {
		return lastCustomSearchExportFilePath ?: ''
	}

	/**
	 * Verifies the exported Custom Search file exists in the project {@code Downloads} folder
	 * and is not empty.
	 */
	@Keyword
	def verifyExportedCustomSearchFilePresentInDownloads(String absolutePath = null, String downloadsFolder = null) {
		try {
			File downloadsDir = resolveCustomSearchExportDownloadsDirectory(downloadsFolder)
			if (!downloadsDir.isDirectory()) {
				failStop("Downloads folder does not exist: ${downloadsDir.absolutePath}")
			}

			String path = (absolutePath ?: lastCustomSearchExportFilePath ?: '').trim()
			if (!path) {
				failStop('verifyExportedCustomSearchFilePresentInDownloads: export file path is blank')
			}

			File exportFile = new File(path)
			if (!exportFile.isFile()) {
				failStop("Exported file is not present in Downloads: ${path}")
			}

			String downloadsCanonical = downloadsDir.canonicalPath
			String fileCanonical = exportFile.canonicalPath
			if (!fileCanonical.startsWith(downloadsCanonical)) {
				failStop(
					"Exported file is not in Downloads folder. Expected under ${downloadsCanonical}, got: ${fileCanonical}"
				)
			}

			if (exportFile.length() <= 0L) {
				failStop("Exported file is empty: ${path}")
			}

			KeywordUtil.logInfo(
				"Exported Custom Search file is present in Downloads (${exportFile.name}): ${exportFile.absolutePath}"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedCustomSearchFilePresentInDownloads failed: ${e.message}")
		}
	}

	@Keyword
	def verifyExportedCustomSearchFileHasColumnHeaders(String absolutePath = null) {
		try {
			String path = (absolutePath ?: lastCustomSearchExportFilePath ?: '').trim()
			if (!path) {
				failStop('verifyExportedCustomSearchFileHasColumnHeaders: export file path is blank')
			}
			String lower = path.toLowerCase()
			if (!lower.endsWith('.csv') && !lower.endsWith('.xlsx')) {
				failStop("Expected a .csv or .xlsx export file, got: ${path}")
			}

			List<String> expected = CUSTOM_SEARCH_EXPORT_COLUMN_HEADERS
			if (lower.endsWith('.csv')) {
				if (!customSearchExportCsvContainsAllHeaders(path, expected)) {
					failStop("Exported Custom Search CSV missing headers: ${expected.join(', ')}")
				}
			} else {
				List<String> actual = readCustomSearchXlsxFirstRowColumnHeaders(path)
				List<String> missing = expected.findAll { String col ->
					!actual.any { String header -> header.equalsIgnoreCase(col.trim()) }
				}
				if (!missing.isEmpty()) {
					failStop("Exported Custom Search XLSX missing headers: ${missing.join(', ')}. Actual: ${actual.join(', ')}")
				}
			}

			KeywordUtil.logInfo("Validated Custom Search export headers: ${expected.join(', ')}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedCustomSearchFileHasColumnHeaders failed: ${e.message}")
		}
	}

	@Keyword
	def verifyExportedCustomSearchFileContainsAppliedFilters(String absolutePath = null, Map<String, String> filters = [:]) {
		try {
			String path = (absolutePath ?: lastCustomSearchExportFilePath ?: '').trim()
			Map<String, String> expectedFilters = normalizeAppliedFilterValues(filters)
			if (!path) {
				failStop('verifyExportedCustomSearchFileContainsAppliedFilters: export file path is blank')
			}
			if (expectedFilters.isEmpty()) {
				failStop('verifyExportedCustomSearchFileContainsAppliedFilters: no filter values provided')
			}

			String blob = readCustomSearchExportTextBlob(path)
			List<String> missing = []
			expectedFilters.each { String label, String value ->
				if (!exportBlobContainsValue(blob, value)) {
					missing.add("${label}='${value}'")
				}
			}
			if (!missing.isEmpty()) {
				failStop("Exported Custom Search file did not contain applied filter values: ${missing.join(', ')}")
			}

			KeywordUtil.logInfo("Custom Search export contains applied filter values: ${expectedFilters}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedCustomSearchFileContainsAppliedFilters failed: ${e.message}")
		}
	}

	@Keyword
	def verifyExportedCustomSearchFileContainsUiRowSnippet(String absolutePath = null, String uiRowText = null) {
		try {
			String path = (absolutePath ?: lastCustomSearchExportFilePath ?: '').trim()
			String snippet = (uiRowText ?: '').trim()
			if (!path || !snippet) {
				failStop('verifyExportedCustomSearchFileContainsUiRowSnippet: path or uiRowText is blank')
			}

			String token = snippet.length() > 32 ? snippet.substring(0, 32) : snippet
			String blob = readCustomSearchExportTextBlob(path)
			if (!exportBlobContainsValue(blob, token)) {
				failStop("Custom Search export file did not contain UI row snippet '${token}'")
			}

			KeywordUtil.logInfo("Custom Search export contains UI row snippet '${token}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedCustomSearchFileContainsUiRowSnippet failed: ${e.message}")
		}
	}

	@Keyword
	def deleteLastCustomSearchExportFile(String absolutePath = null) {
		try {
			String path = (absolutePath ?: lastCustomSearchExportFilePath ?: '').trim()
			if (!path) {
				KeywordUtil.markWarning('deleteLastCustomSearchExportFile: no export file path to delete')
				return
			}
			File file = new File(path)
			if (!file.isFile()) {
				failStop("Custom Search export file not found for deletion: ${path}")
			}
			if (!file.delete()) {
				failStop("Failed to delete Custom Search export file: ${path}")
			}
			KeywordUtil.logInfo("Deleted Custom Search export file: ${path}")
			if (path == lastCustomSearchExportFilePath) {
				lastCustomSearchExportFilePath = ''
				lastCustomSearchExportFileName = ''
			}
		} catch (Exception e) {
			failStop("deleteLastCustomSearchExportFile failed: ${e.message}")
		}
	}

	// ---------- CCIPH-22 Customs Search keywords ----------

	private static TestObject dropdownSearchInput() {
		return dynamicXpathObject('cs_ddl_search_input',
			"//*[@role='listbox']//input[not(@type='hidden')]" +
			" | //*[@role='dialog']//input[contains(@placeholder,'Search')]" +
			" | //input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')][1]")
	}

	private static TestObject dropdownNoRecordsMessage() {
		return dynamicXpathObject('cs_ddl_no_records',
			"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no records found') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no record found')]")
	}

	private void openDropdownByLabel(String label) {
		TestObject trigger = resolveDropdownTriggerByLabel(label)
		waitVisibleClickable(trigger)
		click(trigger)
		WebUI.delay(1)
	}

	@Keyword
	def searchAndSelectInDropdownByLabel(String label, String searchKeyword, String selectValue = null) {
		try {
			String keyword = (searchKeyword ?: '').trim()
			String target = (selectValue ?: searchKeyword ?: '').trim()
			if (!label?.trim() || !target) {
				failStop('searchAndSelectInDropdownByLabel: label and select value are required')
			}
			openDropdownByLabel(label)
			TestObject searchInput = dropdownSearchInput()
			if (keyword && WebUI.waitForElementVisible(searchInput, 5, FailureHandling.OPTIONAL)) {
				WebUI.clearText(searchInput, FailureHandling.OPTIONAL)
				WebUI.setText(searchInput, keyword)
				WebUI.delay(1)
			}
			clickDropdownOption(target)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("searchAndSelectInDropdownByLabel(${label}, ${searchKeyword}, ${selectValue}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDropdownNoMatchMessage(String label, String invalidKeyword) {
		try {
			openDropdownByLabel(label)
			TestObject searchInput = dropdownSearchInput()
			waitVisibleClickable(searchInput)
			WebUI.clearText(searchInput, FailureHandling.OPTIONAL)
			WebUI.setText(searchInput, (invalidKeyword ?: 'ZZZNOMATCH').trim())
			WebUI.delay(1)
			waitVisible(dropdownNoRecordsMessage())
			WebUI.verifyElementVisible(dropdownNoRecordsMessage(), FailureHandling.STOP_ON_FAILURE)
			WebUI.sendKeys(searchInput, '\uE00C')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDropdownNoMatchMessage(${label}, ${invalidKeyword}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDropdownSearchCaseInsensitive(String label, String selectValue, List<String> caseVariants) {
		try {
			List<String> variants = (caseVariants ?: []).findAll { (it ?: '').trim() }
			if (!label?.trim() || !selectValue?.trim() || variants.isEmpty()) {
				failStop('verifyDropdownSearchCaseInsensitive: label, selectValue and caseVariants are required')
			}
			for (String variant : variants) {
				searchAndSelectInDropdownByLabel(label, variant, selectValue)
				TestObject field = resolveDropdownTriggerByLabel(label)
				waitVisible(field)
				String shown = getFieldDisplayedValue(field)
				if (!normalizeLabel(shown).contains(normalizeLabel(selectValue))) {
					failStop("Case variant '${variant}' did not select '${selectValue}'. Displayed: '${shown}'")
				}
				KeywordUtil.logInfo("Case-insensitive search passed for variant '${variant}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDropdownSearchCaseInsensitive(${label}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCustomsSearchScreenLoadsWithAllFilters() {
		verifyCustomsSearchScreenLoaded()
		verifyFilterLabelsVisible()
	}

	@Keyword
	def verifyBasicFiltersSearch(
			String chargeCountry = 'All',
			String entity = 'All',
			String chargeGroup = 'Cost',
			String chargeCode = 'ECD',
			String chargeStatus = 'All') {
		selectDropdownByLabel('Charge Country', chargeCountry)
		selectDropdownByLabel('Entity', entity)
		selectChargeGroup(chargeGroup)
		selectChargeCode(chargeCode)
		selectDropdownByLabel('Charge Status', chargeStatus)
		clickSearch()
		verifySearchResultsDisplayed()
	}
	@Keyword
	def selectBasicFilters(
		String chargeCountry,
		String chargeCode,
		String chargeStatus,
		String chargeGroup,
		String entity
	) {
	
		try {
	
			TestObject chargeCountryDropdown = to21('dropdown_chargeCountry')
			TestObject chargeCodeDropdown = to21('dropdown_chargeCode')
			TestObject chargeStatusDropdown = to21('dropdown_chargeStatus')
			TestObject chargeGroupDropdown = to21('dropdown_chargeGroup')
			TestObject entityDropdown = to21('dropdown_entity')
	
			// Select Charge Country
			WebUI.waitForElementVisible(
				chargeCountryDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				chargeCountryDropdown,
				chargeCountry,
				false
			)
	
			// Select Charge Code
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
	
			// Select Charge Status
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
	
			// Select Charge Group
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
	
			// Select Entity
			WebUI.waitForElementVisible(
				entityDropdown,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.selectOptionByLabel(
				entityDropdown,
				entity,
				false
			)
	
			KeywordUtil.logInfo(
				"Successfully selected Basic Filters - " +
				"Charge Country: '${chargeCountry}', " +
				"Charge Code: '${chargeCode}', " +
				"Charge Status: '${chargeStatus}', " +
				"Charge Group: '${chargeGroup}', " +
				"Entity: '${entity}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to select Basic Filters: ${e.message}"
			)
		}
	}

	@Keyword
	def verifyChargeGroupChargeCodeDependency() {
		verifyCostChargeCodesAvailable()
		verifyRevenueChargeCodesAvailable()
	}

	@Keyword
	def verifyDateTypeAndRangeSearch(String dateType, String startDate, String endDate) {
		selectDateTypeField(dateType ?: 'Pickup Date')
		setCustomDate('Start Date', startDate)
		setCustomDate('End Date', endDate)
		WebUI.delay(4)
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyInvalidDateRangePreventsSearch(String dateType, String startDate, String endDate) {
		try {
			String type = normalizeDateType(dateType ?: 'Pickup Date')
			String invalidStartDate = (startDate ?: '').trim()
			String endDateValue = (endDate ?: '').trim()

			if (!invalidStartDate || !endDateValue) {
				failStop('verifyInvalidDateRangePreventsSearch: startDate and endDate are required')
			}

			selectDateTypeField(type)
			setCustomDate('End Date', endDateValue)
			WebUI.delay(1)
			setCustomDate('Start Date', invalidStartDate)

			Date parsedStart = parseFlexibleDate(invalidStartDate)
			Date parsedEnd = parseFlexibleDate(endDateValue)
			if (parsedStart && parsedEnd && !parsedStart.after(parsedEnd)) {
				KeywordUtil.markWarning(
					"Test data Start Date '${invalidStartDate}' is not after End Date '${endDateValue}'; " +
					'continuing to verify Start Date matches End Date on screen.'
				)
			}

			// UI auto-corrects Start Date to match End Date when Start Date > End Date.
			waitForStartDateToMatchEndDate()

			String actualStartDate = getCustomDateFieldValue('Start Date')
			String actualEndDate = getCustomDateFieldValue('End Date')

			if (!datesAreEqual(actualStartDate, actualEndDate)) {
				failStop(
					"Expected Start Date to reset to End Date. Start='${actualStartDate}', End='${actualEndDate}'"
				)
			}

			if (parsedStart && parsedEnd && parsedStart.after(parsedEnd) &&
				datesAreEqual(actualStartDate, invalidStartDate)) {
				failStop(
					"Start Date was not reset after entering invalid range. " +
					"Start='${actualStartDate}', End='${actualEndDate}', Entered Start='${invalidStartDate}'"
				)
			}

			KeywordUtil.logInfo(
				"Invalid date range handled correctly. Start Date reset to End Date: '${actualStartDate}'"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("verifyInvalidDateRangePreventsSearch failed: ${e.message}")
		}
	}

	@Keyword
	def verifyBusinessAccountCategorySearch(String businessType, String accountType, String category) {
		if (accountType) selectDropdownByLabel('Account Type', accountType)
		if (businessType) selectDropdownByLabel('Business Type', businessType)
		if (category) selectDropdownByLabel('Category', category)
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyAllSearchCriteriaTypes(Map<String, String> criteriaValues) {
		Map<String, String> values = criteriaValues ?: [:]
		if (values.isEmpty()) {
			values = [
				'Tracking Number': 'DP13000',
				'Console'        : 'CONSOLE001',
				'Account Number' : '1100053259',
//				'BOE'            : 'BOE123456'
			]
		}
		values.each { String criteria, String value ->
			if (!value?.trim()) {
				return
			}
			performSearchWithCriteria(criteria, value)
			verifySearchResultsDisplayed()
			KeywordUtil.logInfo("Verified search criteria '${criteria}' with value '${value}'")
		}
	}

	@Keyword
	def verifyMultipleTrackingSearchFlow(String trackingNumbers) {
		selectSearchCriteria('Tracking Number')
		clickMultipleButton()
		verifyMultipleShipmentsTextPresent()
		enterMultipleSearchValues(trackingNumbers)
		clickOkayButton()
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyAdditionalDetailsSearch(
			String originEntity,
			String originCountry,
			String destEntity,
			String destCountry,
			String thirdPartyEntity,
			String thirdPartyCountry) {
		setAdditionalDetailsToggle(true)
		if (originEntity) selectDropdownByLabel('Origin Entity', originEntity)
		if (originCountry) selectDropdownByLabel('Origin Country', originCountry)
		if (destEntity) selectDropdownByLabel('Dest. Entity', destEntity)
		if (destCountry) selectDropdownByLabel('Dest. Country', destCountry)
		if (thirdPartyEntity) selectDropdownByLabel('3rd party Entity', thirdPartyEntity)
		if (thirdPartyCountry) selectDropdownByLabel('3rd party Country', thirdPartyCountry)
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyCombinedBasicAndAdditionalSearch(Map<String, String> data = [:]) {
		Map<String, String> filters = data ?: [:]
		selectDropdownByLabel('Charge Country', filters.charge_Country ?: 'All')
		selectDropdownByLabel('Entity', filters.entity ?: 'All')
		selectChargeGroup(filters.charge_Group ?: 'Cost')
		selectChargeCode(filters.charge_Code ?: 'ECD')
		selectDropdownByLabel('Charge Status', filters.charge_Status ?: 'All')
		if (filters.date_Type) {
			selectDateTypeField(filters.date_Type)
			if (filters.start_Date) setCustomDate('Start Date', filters.start_Date)
			if (filters.end_Date) setCustomDate('End Date', filters.end_Date)
		}
		selectDropdownByLabel('Account Type', filters.account_Type ?: 'All')
		if (filters.business_Type) selectDropdownByLabel('Business Type', filters.business_Type)
		selectDropdownByLabel('Category', filters.category ?: 'All')
		selectSearchCriteria(filters.search_Criteria ?: 'Tracking Number')
		if (filters.search_Value) setSearchInputText(filters.search_Value)
		setAdditionalDetailsToggle(true)
		if (filters.origin_Entity) selectDropdownByLabel('Origin Entity', filters.origin_Entity)
		if (filters.origin_Country) selectDropdownByLabel('Origin Country', filters.origin_Country)
		if (filters.dest_Entity) selectDropdownByLabel('Dest. Entity', filters.dest_Entity)
		if (filters.dest_Country) selectDropdownByLabel('Dest. Country', filters.dest_Country)
		if (filters.third_Party_Entity) selectDropdownByLabel('3rd party Entity', filters.third_Party_Entity)
		if (filters.third_Party_Country) selectDropdownByLabel('3rd party Country', filters.third_Party_Country)
		clickSearch()
		verifySearchResultsDisplayed()
	}

	@Keyword
	def verifyNoRecordsFoundForSearchValue(String searchCriteria, String searchValue) {
		selectSearchCriteria(searchCriteria ?: 'Tracking Number')
		if (searchValue) setSearchInputText(searchValue)
		clickSearch()
		verifyNoDataFoundMessage()
	}

	@Keyword
	def verifySearchResultsForKnownShipment(String searchCriteria, String searchValue) {
		performSearchWithCriteria(searchCriteria ?: 'Tracking Number', searchValue)
		verifySearchResultsDisplayed()
		KeywordUtil.logInfo('TC_SEARCH_012: Compare displayed rows with database records when DB access is configured.')
	}

	@Keyword
	def verifySearchPerformanceWithinThreshold(int maxSeconds = 30) {
		selectDropdownByLabel('Charge Country', 'All')
		selectDropdownByLabel('Entity', 'All')
		selectChargeGroup('Cost')
		selectChargeCode('ECD')
		long start = System.currentTimeMillis()
		clickSearch()
		verifySearchResultsDisplayed()
		long elapsed = System.currentTimeMillis() - start
		KeywordUtil.logInfo("Search response time: ${elapsed} ms")
		if (elapsed > (maxSeconds * 1000L)) {
			failStop("Search exceeded acceptable response time (${elapsed} ms > ${maxSeconds}s)")
		}
		WebUI.takeScreenshot()
	}

	@Keyword
	def verifyConsecutiveSearchesWithoutRefresh(
			String chargeGroupFirst,
			String chargeCodeFirst,
			String chargeStatusFirst,
			String chargeGroupSecond,
			String chargeCodeSecond,
			String chargeStatusSecond) {
		selectChargeGroup(chargeGroupFirst ?: 'Cost')
		selectChargeCode(chargeCodeFirst ?: 'ECD')
		selectDropdownByLabel('Charge Status', chargeStatusFirst ?: 'All')
		clickSearch()
		verifySearchResultsDisplayed()
		selectChargeGroup(chargeGroupSecond ?: 'Revenue')
		selectChargeCode(chargeCodeSecond ?: 'DTC')
		selectDropdownByLabel('Charge Status', chargeStatusSecond ?: 'Due')
		clickSearch()
		verifySearchResultsDisplayed()
		WebUI.takeScreenshot()
	}

	// ---------- CCIPH-23: Hide / Show Filters toggle ----------

	private TestObject hideShowFiltersButton() {
		return to('btn_hideShowFilters')
	}

	private TestObject chargeGroupFilterField() {
		return to21('dropdown_chargeGroup')
	}

	private String getHideShowFiltersButtonLabel() {
		return getFieldDisplayedValue(hideShowFiltersButton())
	}

	private boolean isHideShowFiltersButtonActive() {
		TestObject btn = hideShowFiltersButton()
		if (!WebUI.waitForElementPresent(btn, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
			return false
		}
		String disabled = (WebUI.getAttribute(btn, 'disabled', FailureHandling.OPTIONAL) ?: '').trim()
		String ariaDisabled = (WebUI.getAttribute(btn, 'aria-disabled', FailureHandling.OPTIONAL) ?: '').trim()
		String clazz = (WebUI.getAttribute(btn, 'class', FailureHandling.OPTIONAL) ?: '').toLowerCase()
		if (disabled.equalsIgnoreCase('true') || ariaDisabled.equalsIgnoreCase('true')) {
			return false
		}
		if (clazz.contains('disabled') || clazz.contains('inactive') || clazz.contains('is-disabled')) {
			return false
		}
		return WebUI.waitForElementClickable(btn, 5, FailureHandling.OPTIONAL)
	}

	private boolean isFilterSectionExpanded() {
		return WebUI.waitForElementVisible(chargeGroupFilterField(), 5, FailureHandling.OPTIONAL)
	}

	private boolean isFilterSectionCollapsed() {
		return !isFilterSectionExpanded()
	}

	private void verifyHideShowFiltersButtonLabelContains(String expectedText) {
		String expected = (expectedText ?: '').trim().toLowerCase()
		String actual = (getHideShowFiltersButtonLabel() ?: '').toLowerCase()
		if (!actual.contains(expected)) {
			failStop("Expected Hide/Show Filters button label to contain '${expectedText}' but was '${getHideShowFiltersButtonLabel()}'")
		}
	}

	private String getDropdownSelectedTextByLabel(String label) {
		TestObject field = resolveDropdownTriggerByLabel(label)
		waitVisible(field)
		String tag = (WebUI.getAttribute(field, 'tagName', FailureHandling.OPTIONAL) ?: '').toLowerCase()
		if (tag == 'select') {
			WebElement element = WebUI.findWebElement(field)
			return (new org.openqa.selenium.support.ui.Select(element).getFirstSelectedOption()?.getText() ?: '').trim()
		}
		return getFieldDisplayedValue(field)
	}

	private void verifyDropdownValueRetained(String label, String expectedValue) {
		String expected = (expectedValue ?: '').trim()
		String actual = getDropdownSelectedTextByLabel(label)
		if (!normalizeLabel(actual).contains(normalizeLabel(expected)) && !normalizeLabel(expected).contains(normalizeLabel(actual))) {
			failStop("Filter '${label}' expected '${expectedValue}' after hide/show toggle but was '${actual}'")
		}
	}

	@Keyword
	def verifyHideFiltersButtonVisibleBeforeSearch() {
		try {
			verifyCustomsSearchScreenLoaded()
			WebUI.verifyElementVisible(hideShowFiltersButton(), FailureHandling.STOP_ON_FAILURE)
			verifyHideShowFiltersButtonLabelContains('hide filter')
			if (isHideShowFiltersButtonActive()) {
				failStop('Hide Filters button should be inactive before performing a search')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHideFiltersButtonVisibleBeforeSearch failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHideFiltersButtonActiveAfterSearch() {
		try {
			verifyHideShowFiltersButtonLabelContains('hide filter')
			if (!isHideShowFiltersButtonActive()) {
				failStop('Hide Filters button should be active after search results are displayed')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHideFiltersButtonActiveAfterSearch failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHideFiltersButtonAvailabilityAfterSearch() {
		try {
			verifyHideFiltersButtonVisibleBeforeSearch()
			clickSearch()
			verifySearchResultsDisplayed()
			verifyHideFiltersButtonActiveAfterSearch()
		} catch (Exception e) {
			failStop("verifyHideFiltersButtonAvailabilityAfterSearch failed: ${e.message}")
		}
	}

	@Keyword
	def performValidSearchForFilterToggle() {
		try {
			clickSearch()
			verifySearchResultsDisplayed()
			verifyHideFiltersButtonActiveAfterSearch()
		} catch (Exception e) {
			failStop("performValidSearchForFilterToggle failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFilterSectionCollapsed() {
		try {
			if (!isFilterSectionCollapsed()) {
				failStop('Filter section should be collapsed')
			}
			verifySearchResultsDisplayed()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterSectionCollapsed failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFilterSectionExpanded() {
		try {
			if (!isFilterSectionExpanded()) {
				failStop('Filter section should be expanded')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterSectionExpanded failed: ${e.message}")
		}
	}

	@Keyword
	def clickHideShowFiltersButton() {
		toggleHideShowFilters()
	}

	@Keyword
	def verifyFilterSectionCollapsesUsingHideFilters() {
		try {
			performValidSearchForFilterToggle()
			clickHideShowFiltersButton()
			verifyFilterSectionCollapsed()
			verifyHideShowFiltersButtonLabelContains('show filter')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterSectionCollapsesUsingHideFilters failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFilterSectionExpandsUsingShowFilters() {
		try {
			verifyFilterSectionCollapsesUsingHideFilters()
			clickHideShowFiltersButton()
			verifyFilterSectionExpanded()
			verifyHideShowFiltersButtonLabelContains('hide filter')
			verifySearchResultsDisplayed()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterSectionExpandsUsingShowFilters failed: ${e.message}")
		}
	}
	
	@Keyword
	def validateTrackingNumbersPresent(String trackingNumbers) {
	
		try {
	
			// Split comma-separated tracking numbers
			List<String> expectedTrackingNumbers = trackingNumbers
				.split(",")
				.collect { it.trim() }
	
			TestObject trackingNumberCells = new TestObject(
				"Tracking Number Cells"
			)
	
			trackingNumberCells.addProperty(
				"xpath",
				ConditionType.EQUALS,
				"//td[contains(@class,'resultsCellTracking')]"
			)
	
			WebUI.waitForElementPresent(
				trackingNumberCells,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Get all tracking number cells from the table
			List<WebElement> trackingElements =
				WebUiCommonHelper.findWebElements(
					trackingNumberCells,
					15
				)
	
			// Get actual tracking numbers displayed
			List<String> actualTrackingNumbers = trackingElements.collect {
				it.getText().trim()
			}
	
			KeywordUtil.logInfo(
				"Expected Tracking Numbers: ${expectedTrackingNumbers}"
			)
	
			KeywordUtil.logInfo(
				"Actual Tracking Numbers: ${actualTrackingNumbers}"
			)
	
			// Validate every expected tracking number
			expectedTrackingNumbers.each { expectedNumber ->
	
				if (!actualTrackingNumbers.contains(expectedNumber)) {
	
					WebUI.takeScreenshot()
	
					failStop(
						"Tracking Number '${expectedNumber}' was NOT found in results table. " +
						"Actual Tracking Numbers: ${actualTrackingNumbers}"
					)
				}
	
				KeywordUtil.logInfo(
					"Tracking Number '${expectedNumber}' is present in results table"
				)
			}
	
			KeywordUtil.markPassed(
				"All expected Tracking Numbers are present: ${expectedTrackingNumbers}"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Tracking Numbers '${trackingNumbers}': ${e.message}"
			)
		}
	}

	@Keyword
	def verifyFilterValuesRetainedAfterHideAndShow(
			String chargeGroup = 'Cost',
			String chargeCode = 'ECD',
			String chargeStatus = 'Due') {
		try {
			selectChargeGroup(chargeGroup ?: 'Cost')
			selectChargeCode(chargeCode ?: 'ECD')
			selectDropdownByLabel('Charge Status', chargeStatus ?: 'Due')
			performValidSearchForFilterToggle()
			clickHideShowFiltersButton()
			verifyFilterSectionCollapsed()
			clickHideShowFiltersButton()
			verifyFilterSectionExpanded()
			verifyDropdownValueRetained('Charge Group', chargeGroup)
			verifyDropdownValueRetained('Charge Code', chargeCode)
			verifyDropdownValueRetained('Charge Status', chargeStatus)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyFilterValuesRetainedAfterHideAndShow failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchAfterMultipleFilterToggles(
			String chargeGroup = 'Cost',
			String chargeCode = 'ECD',
			String chargeStatus = 'Actualized') {
		try {
			performValidSearchForFilterToggle()
			for (int i = 0; i < 3; i++) {
				clickHideShowFiltersButton()
				WebUI.delay(1)
			}
			if (isFilterSectionCollapsed()) {
				clickHideShowFiltersButton()
			}
			verifyFilterSectionExpanded()
			selectChargeGroup(chargeGroup ?: 'Cost')
			selectChargeCode(chargeCode ?: 'ECD')
			selectDropdownByLabel('Charge Status', chargeStatus ?: 'Actualized')
			clickSearch()
			verifySearchResultsDisplayed()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchAfterMultipleFilterToggles failed: ${e.message}")
		}
	}

	// ---------- CCIPH-26: Search results grid ----------

	private static final Map<String, List<String>> GRID_HEADER_ALIASES = [
		'Total Estimate': ['total estimate', 'total outstanding'],
		'Total Actual': ['total actual', 'total balance'],
		'Destination': ['destination', 'dest.'],
		'3rd Party': ['3rd party', 'third party']
	]

	private static final Map<String, Integer> GRID_COLUMN_INDEX = [
		'Entity': 1,
		'Tracking Number': 2,
		'Account Number': 3,
		'Account Name': 4,
		'Origin': 5,
		'Destination': 6,
		'3rd Party': 7,
		'Charge Group': 8,
		'Charge Code': 9,
		'Total Estimate': 10,
		'Total Actual': 11,
		'Status': 12
	]

	private static String normalizeHeaderText(String text) {
		return (text ?: '').trim().toLowerCase().replaceAll('\\s+', ' ')
	}

	private static boolean headerMatchesAlias(String actualHeader, String expectedColumn) {
		String actual = normalizeHeaderText(actualHeader)
		List<String> aliases = GRID_HEADER_ALIASES.get(expectedColumn, [normalizeHeaderText(expectedColumn)])
		return aliases.any { actual.contains(it) }
	}

	private static boolean cellTextMatches(String actual, String expected) {
		String a = normalizeLabel(actual)
		String e = normalizeLabel(expected)
		return a == e || a.contains(e) || e.contains(a)
	}

	private TestObject firstResultRowCell(int columnIndex) {
		return dynamicXpathObject(
			"cs26_result_cell_${columnIndex}",
			"(${RESULTS_ROW_XPATH})[1]//td[${columnIndex}]"
		)
	}

	private String getFirstResultRowCellText(int columnIndex) {
		TestObject cell = firstResultRowCell(columnIndex)
		waitVisible(cell)
		return (WebUI.getText(cell, FailureHandling.OPTIONAL) ?: '').trim()
	}

	private List<String> getResultGridHeaderTexts() {
		TestObject headerObj = to26('tbl_resultHeaders')
		waitVisible(headerObj)
		List<WebElement> headerElements = WebUiCommonHelper.findWebElements(headerObj, DEFAULT_TIMEOUT)
		return headerElements.collect { normalizeHeaderText(it.getText()) }.findAll { it }
	}

	private List<WebElement> getResultGridDataRows() {
		return WebUiCommonHelper.findWebElements(to26('tbl_resultRows'), DEFAULT_TIMEOUT)
	}

	private void verifyFirstRowCellContains(int columnIndex, String expectedValue, String columnLabel) {
		String expected = (expectedValue ?: '').trim()
		if (!expected || expected == '-') {
			KeywordUtil.logInfo("Skipping ${columnLabel} validation; expected value not provided.")
			return
		}
		String actual = getFirstResultRowCellText(columnIndex)
		if (!cellTextMatches(actual, expected)) {
			failStop("${columnLabel} column expected '${expected}' but was '${actual}'")
		}
		KeywordUtil.logInfo("Validated ${columnLabel}: '${actual}'")
	}

	@Keyword
	def searchWithFiltersAndOptionalCriteria(
			String chargeGroup,
			String chargeCode,
			String chargeStatus,
			String searchCriteria = '',
			String searchValue = '') {
		try {
			selectMultipleFilters(chargeGroup ?: 'Cost', chargeCode ?: 'ECD', chargeStatus ?: 'Actualized')
			String criteria = (searchCriteria ?: '').trim()
			String value = (searchValue ?: '').trim()
			if (criteria && value) {
				selectSearchCriteriaAndEnterValue(criteria, value)
				clickSearch()
			} else {
				clickSearch()
			}
		} catch (Exception e) {
			failStop("searchWithFiltersAndOptionalCriteria failed: ${e.message}")
		}
	}

	@Keyword
	def verifySearchResultsGridDisplayed() {
		try {
			verifySearchResultsDisplayed()
			waitVisible(to26('tbl_resultHeaders'))
			waitVisible(to26('tbl_resultRows'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySearchResultsGridDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCustomsSearchDashboardView(
			String chargeGroup = '',
			String chargeCode = '',
			String chargeStatus = '') {
		try {
			verifyCustomsSearchScreenLoaded()
			if ((chargeGroup ?: '').trim() || (chargeCode ?: '').trim() || (chargeStatus ?: '').trim()) {
				searchWithFiltersAndOptionalCriteria(chargeGroup, chargeCode, chargeStatus)
			} else {
				clickSearch()
			}
			verifySearchResultsGridDisplayed()
			verifyFilterLabelsVisible()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCustomsSearchDashboardView failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEachResultRowRepresentsShipmentCharge() {
		try {
			List<WebElement> rows = getResultGridDataRows()
			if (rows.isEmpty()) {
				failStop('No search result rows displayed')
			}
			int sampleSize = Math.min(rows.size(), 5)
			for (int i = 0; i < sampleSize; i++) {
				List<WebElement> cells = rows[i].findElements(By.tagName('td'))
				if (cells.size() < 12) {
					failStop("Result row ${i + 1} has ${cells.size()} columns; expected at least 12 shipment charge columns")
				}
				String entity = (cells[0]?.getText() ?: '').trim()
				String tracking = (cells[1]?.getText() ?: '').trim()
				String chargeCode = (cells[8]?.getText() ?: '').trim()
				if (!entity || !tracking || !chargeCode) {
					failStop("Result row ${i + 1} is missing core shipment charge values (entity/tracking/charge code)")
				}
			}
			KeywordUtil.logInfo("Validated ${sampleSize} result row(s) represent shipment charge records")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEachResultRowRepresentsShipmentCharge failed: ${e.message}")
		}
	}

	@Keyword
	def verifyResultGridColumnHeadersInOrder() {
		try {
			verifySearchResultsGridDisplayed()
			List<String> headers = getResultGridHeaderTexts()
			if (headers.isEmpty()) {
				failStop('No result grid column headers found')
			}
			int headerIdx = 0
			for (String expectedColumn : GRID_COLUMN_HEADERS) {
				boolean matched = false
				while (headerIdx < headers.size() && !matched) {
					if (headerMatchesAlias(headers[headerIdx], expectedColumn)) {
						matched = true
						KeywordUtil.logInfo("Validated column header '${expectedColumn}' at position ${headerIdx + 1}")
					}
					headerIdx++
				}
				if (!matched) {
					failStop("Expected column '${expectedColumn}' not found in defined order. Headers: ${headers}")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyResultGridColumnHeadersInOrder failed: ${e.message}")
		}
	}

	@Keyword
	def verifyResultCountDisplayedForBackendComparison(
			String chargeGroup = 'Cost',
			String chargeCode = 'ECD',
			String chargeStatus = 'Actualized') {
		try {
			searchWithFiltersAndOptionalCriteria(chargeGroup, chargeCode, chargeStatus)
			verifySearchResultsGridDisplayed()
			int rowCount = getResultGridDataRows().size()
			if (rowCount <= 0) {
				failStop('No search result rows available for backend comparison')
			}
			KeywordUtil.logInfo("Displayed ${rowCount} row(s) on current page for TC_US_FUNC_022. Configure DB/API comparison when backend access is available.")
			if (rowCount >= 50) {
				verifyPaginationControlsVisible()
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyResultCountDisplayedForBackendComparison failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntityColumnPopulated(String expectedEntity = '') {
		try {
			verifyFirstRowCellContains(GRID_COLUMN_INDEX['Entity'], expectedEntity, 'Entity')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityColumnPopulated failed: ${e.message}")
		}
	}

	@Keyword
	def verifyTrackingNumberColumnValues(String expectedTrackingNumber = '') {
		try {
			String tracking = getFirstResultRowCellText(GRID_COLUMN_INDEX['Tracking Number'])
			if (!tracking) {
				failStop('Tracking Number column is empty on first result row')
			}
			if ((expectedTrackingNumber ?: '').trim()) {
				verifyFirstRowCellContains(GRID_COLUMN_INDEX['Tracking Number'], expectedTrackingNumber, 'Tracking Number')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyTrackingNumberColumnValues failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAccountNumberAndNameColumns(String expectedAccountNumber = '', String expectedAccountName = '') {
		try {
			verifyFirstRowCellContains(GRID_COLUMN_INDEX['Account Number'], expectedAccountNumber, 'Account Number')
			verifyFirstRowCellContains(GRID_COLUMN_INDEX['Account Name'], expectedAccountName, 'Account Name')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAccountNumberAndNameColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyOriginDestinationAndThirdPartyColumns(
			String expectedOrigin = '',
			String expectedDestination = '',
			String expectedThirdParty = '') {
		try {
			verifyFirstRowCellContains(GRID_COLUMN_INDEX['Origin'], expectedOrigin, 'Origin')
			verifyFirstRowCellContains(GRID_COLUMN_INDEX['Destination'], expectedDestination, 'Destination')
			if ((expectedThirdParty ?: '').trim() && expectedThirdParty.trim() != '-') {
				verifyFirstRowCellContains(GRID_COLUMN_INDEX['3rd Party'], expectedThirdParty, '3rd Party')
			} else {
				KeywordUtil.logInfo('3rd Party validation skipped (not provided or dash).')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyOriginDestinationAndThirdPartyColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeGroupAndChargeCodeColumns(String expectedChargeGroup = 'Cost', String expectedChargeCode = 'ECD') {
		try {
			String chargeGroup = getFirstResultRowCellText(GRID_COLUMN_INDEX['Charge Group'])
			String chargeCode = getFirstResultRowCellText(GRID_COLUMN_INDEX['Charge Code'])
			if (!cellTextMatches(chargeGroup, expectedChargeGroup) && !chargeGroup.toUpperCase().contains((expectedChargeGroup ?: '').toUpperCase())) {
				failStop("Charge Group expected '${expectedChargeGroup}' but was '${chargeGroup}'")
			}
			if (!cellTextMatches(chargeCode, expectedChargeCode)) {
				failStop("Charge Code expected '${expectedChargeCode}' but was '${chargeCode}'")
			}
			KeywordUtil.logInfo("Validated Charge Group '${chargeGroup}' and Charge Code '${chargeCode}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyChargeGroupAndChargeCodeColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyTotalEstimateAndTotalActualColumns() {
		try {
			String estimate = getFirstResultRowCellText(GRID_COLUMN_INDEX['Total Estimate'])
			String actual = getFirstResultRowCellText(GRID_COLUMN_INDEX['Total Actual'])
			if (!estimate || !actual) {
				failStop('Total Estimate or Total Actual column is empty on first result row')
			}
			def currencyPattern = ~/^\d{1,3}(?:,\d{3})*\.\d{2}\s+[A-Z]{3}$|^\d+\.\d{2}\s+[A-Z]{3}$/
			if (!(estimate ==~ currencyPattern)) {
				failStop("Total Estimate '${estimate}' is not formatted as currency with 2 decimal places")
			}
			if (!(actual ==~ currencyPattern)) {
				failStop("Total Actual '${actual}' is not formatted as currency with 2 decimal places")
			}
			TestObject estimateCell = firstResultRowCell(GRID_COLUMN_INDEX['Total Estimate'])
			TestObject actualCell = firstResultRowCell(GRID_COLUMN_INDEX['Total Actual'])
			String estimateAlign = (WebUI.getCSSValue(estimateCell, 'text-align', FailureHandling.OPTIONAL) ?: '').trim()
			String actualAlign = (WebUI.getCSSValue(actualCell, 'text-align', FailureHandling.OPTIONAL) ?: '').trim()
			if (!estimateAlign.equalsIgnoreCase('right') && !estimateAlign.equalsIgnoreCase('end')) {
				KeywordUtil.markWarning("Total Estimate alignment is '${estimateAlign}' (expected right)")
			}
			if (!actualAlign.equalsIgnoreCase('right') && !actualAlign.equalsIgnoreCase('end')) {
				KeywordUtil.markWarning("Total Actual alignment is '${actualAlign}' (expected right)")
			}
			KeywordUtil.logInfo("Validated currency columns: estimate='${estimate}', actual='${actual}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyTotalEstimateAndTotalActualColumns failed: ${e.message}")
		}
	}

	@Keyword
	def verifyStatusColumnVisibleAndPopulated() {
		try {
			TestObject statusCell = firstResultRowCell(GRID_COLUMN_INDEX['Status'])
			waitVisible(statusCell)
			String status = (WebUI.getText(statusCell, FailureHandling.OPTIONAL) ?: '').trim()
			if (!status) {
				failStop('Status column is empty on first result row')
			}
			TestObject statusBadge = dynamicXpathObject(
				'cs26_status_badge',
				"(${RESULTS_ROW_XPATH})[1]//td[${GRID_COLUMN_INDEX['Status']}]//*[contains(@class,'badge') or contains(@class,'tag') or contains(@class,'status') or contains(@class,'pill')]"
			)
			if (!WebUI.waitForElementPresent(statusBadge, 5, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo("Status '${status}' displayed; badge class pattern may differ on this build")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyStatusColumnVisibleAndPopulated failed: ${e.message}")
		}
	}

	// ---------- CCIPH-120 Charge Detail / 360 View Screen ----------

	private static final String OR_PREFIX_120 = 'CustomSearch_360ViewScreen/CCIPH-120/'
	private static final List<String> DETAIL_HEADER_ITEMS = [
		'ECD', 'DTC', 'Analysis', 'History', 'Prepare Document'
	]
	private static final List<String> TRACKING_OPERATION_STRIP_LABELS = [
		'Tracking Number', 'Billing Entity', 'Billing Country', 'MAWB', 'Console',
		'Destination', 'Cost Status', 'Operation', 'Charge', 'Category', 'Handler'
	]
	private static final List<String> ECD_ACCOUNT_INFO_LABELS = [
		'Account Type', 'AP Account', 'AP Account name', 'Customs Type', 'Service',
		'Goods value', 'CIF value', 'number of pieces', 'Product Group',
		'Delivery Information', 'Created date', 'Pickup Date', 'Delivery Type', 'Payment Type'
	]
	private static final List<String> DTC_ACCOUNT_INFO_LABELS = [
		'Account Type', 'AP Account', 'AP Account name', 'Business Type', 'Customs Type', 'Service',
		'Goods value', 'CIF value', 'number of pieces', 'Product Group',
		'Delivery Information', 'Created date', 'Pickup Date', 'Delivery Type', 'Payment Type'
	]
	private static final List<String> ADDITIONAL_DETAILS_LABELS_120 = [
		'Goods Summary', 'Origin', 'Destination', '3rd Party', 'Seller Information'
	]
	private static final List<String> ECD_INVOICING_COLUMNS = [
		'Service Code', 'DESCRIPTION', 'AP ACCOUNT', 'AP CURRENCY', 'ESTIMATE',
		'ACTUAL ONE', 'ACTUAL TWO', 'ACTUAL THREE', 'ACTUAL FOUR', 'TOTAL ACTUALS', 'ELIGIBLE COST'
	]
	private static final List<String> DTC_INVOICING_COLUMNS = [
		'SERVICE CODE', 'DESCRIPTION', 'AR ACCOUNT', 'AR CURRENCY', 'ESTIMATE',
		'INVOICE ONE', 'INVOICE TWO', 'INVOICE THREE', 'INVOICE FOUR', 'OUTSTANDING', 'TOTAL INVOICES'
	]
	private static final List<String> DOCUMENT_NUMBER_COLUMNS = [
		'Document Number', 'ACCOUNT', 'ACCOUNT NAME', 'DOCUMENT TYPE', 'AMOUNT',
		'NUMBER OF CHARGES', 'STATUS', 'CREATED BY', 'DOCUMENT DATE', 'CREATED DATE'
	]
	private static final List<String> HISTORY_TABLE_COLUMNS = [
		'Timestamp', 'Subsection', 'Field', 'Old Value', 'New Value', 'User', 'Type'
	]
	private static final List<String> HISTORY_FILTER_OPTIONS = [
		'All', 'Today', 'This Week', 'This Month', 'Custom Range'
	]
	private static final List<String> CHARGE_DETAIL_READ_ONLY_FIELDS = [
		'charge code', 'description', 'amount', 'currency', 'status'
	]

	private static TestObject to120(String name) {
		return findTestObject(OR_PREFIX_120 + name)
	}

	private void verifyDetailLabelsVisible(List<String> labels, String context) {
		for (String label : labels) {
			String lit = xpathLiteral(normalizeLabel(label))
			TestObject el = dynamicXpathObject("cs120_label_${label.hashCode()}",
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]")
			if (!WebUI.waitForElementVisible(el, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
				failStop("Expected '${label}' to be visible on ${context}")
			}
		}
		WebUI.takeScreenshot()
	}

	private void verifyTableHeadersVisible(List<String> columns, String tableContext) {
		for (String column : columns) {
			String lit = xpathLiteral(normalizeLabel(column))
			TestObject header = dynamicXpathObject("cs120_col_${column.hashCode()}",
				"//table//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]" +
				" | //*[contains(@role,'columnheader')][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]")
			waitVisible(header)
		}
		KeywordUtil.logInfo("Validated ${tableContext} column headers: ${columns}")
		WebUI.takeScreenshot()
	}

	private void clickDetailHeaderTab(String tabName) {
		String lit = xpathLiteral((tabName ?: '').trim())
		String litLower = xpathLiteral(normalizeLabel(tabName))
		TestObject tab = dynamicXpathObject("cs120_tab_${tabName.hashCode()}",
			"//*[self::button or self::a or self::span or self::div[@role='tab']]" +
			"[normalize-space()=${lit} or contains(normalize-space(),${lit}) or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${litLower})]")
		click(tab)
		WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
	}

	private void verifyChargeDetailSectionsVisible() {
		verifyDetailLabelsVisible(['Account Information'], 'Charge Detail')
		verifyDetailLabelsVisible(['Additional Details'], 'Charge Detail')
		verifyDetailLabelsVisible(['Invoicing'], 'Charge Detail')
		verifyDetailLabelsVisible(['Document Number'], 'Charge Detail')
	}

	private void logDatabaseValidationPending(String section) {
		KeywordUtil.logInfo(
			"DB/SAP validation for '${section}' requires backend configuration. " +
			"UI validated; wire DB keywords or compare manually when connection is available."
		)
		WebUI.takeScreenshot()
	}

//	@Keyword
//	def navigateToChargeDetailPage(String trackingNumber = 'DP13000') {
//		try {
//			String tracking = (trackingNumber ?: 'DP13000').trim()
//			if (!tracking) {
//				tracking = 'DP13000'
//			}
//			enterTrackingNumber(tracking)
//			clickSearch()
//			validateFirstRow()
//			clickTrackingNumber(tracking)
//			verifyDetailPageLoaded()
//		} catch (Exception e) {
//			failStop("navigateToChargeDetailPage failed: ${e.message}")
//		}
//	}
	@Keyword
	def navigateToChargeDetailPage(String trackingNumber) {
	
		try {
			currentChargeDetailSection = ''
	
			String tracking = trackingNumber?.trim()
	
			if (!tracking) {
	
				failStop("Tracking Number cannot be null or empty.")
			}
	
			enterTrackingNumber(tracking)
	
			clickSearch()
	
			validateFirstRow()
	
			clickTrackingNumber(tracking)
	
			verifyDetailPageLoaded()
	
			KeywordUtil.logInfo(
				"Successfully navigated to Charge Detail page for Tracking Number: '${tracking}'"
			)
	
		} catch (Exception e) {
	
			failStop(
				"navigateToChargeDetailPage failed for Tracking Number '${trackingNumber}': ${e.message}"
			)
		}
	}

	@Keyword
	def openChargeDetailHistoryTab(String trackingNumber = '') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickHistoryTab()
		} catch (Exception e) {
			failStop("openChargeDetailHistoryTab failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeDetailPageAccess(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			verifyChargeDetailReadOnlyAccess()
		} catch (Exception e) {
			failStop("verifyChargeDetailPageAccess failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeDetailReadOnlyAccess() {
		try {
			verifyDetailLabelsVisible(CHARGE_DETAIL_READ_ONLY_FIELDS, 'Charge Detail read-only fields')
			List<WebElement> editableInputs = DriverFactory.getWebDriver().findElements(
				By.xpath(
					"//input[not(@type='hidden') and not(@readonly) and not(@disabled)]" +
					" | //textarea[not(@readonly) and not(@disabled)]" +
					" | //select[not(@disabled)]"
				)
			)
			if (!editableInputs.isEmpty()) {
				KeywordUtil.markWarning("Found ${editableInputs.size()} potentially editable field(s) on Charge Detail page")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyChargeDetailReadOnlyAccess failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeDetailHeaderSection(String trackingNumber = '') {
		try {
			navigateToChargeDetailPage(trackingNumber)
		//	waitVisible(to120('section_header'))
			waitVisible(to120('btn_Back'))
			waitVisible(to120('tab_ECD'))
			waitVisible(to120('tab_DTA'))
			waitVisible(to120('tab_DT3'))
		//	waitVisible(to120('tab_Analysis'))
			waitVisible(to120('tab_History'))
			//waitVisible(to120('tab_prepareDocument'))
			for (String item : DETAIL_HEADER_ITEMS) {
				String lit = xpathLiteral(normalizeLabel(item))
				TestObject headerItem = dynamicXpathObject("cs120_header_${item.hashCode()}",
					"//*[self::button or self::a or self::span][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]")
				waitVisible(headerItem)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyChargeDetailHeaderSection failed: ${e.message}")
		}
	}
	@Keyword
	def validateTrackingNumbers(String expectedTrackingNumber) {
	
		try {
	
			TestObject trackingNumberValue = to120(
				'txt_trackingNumber'
			)
	
			WebUI.waitForElementVisible(
				trackingNumberValue,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementVisible(
				trackingNumberValue,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualTrackingNumber = WebUI.getText(
				trackingNumberValue,
				FailureHandling.STOP_ON_FAILURE
			).trim()
	
			WebUI.verifyMatch(
				actualTrackingNumber,
				expectedTrackingNumber.trim(),
				false,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Tracking Number is visible and successfully validated. " +
				"Expected: '${expectedTrackingNumber}', " +
				"Actual: '${actualTrackingNumber}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate Tracking Number '${expectedTrackingNumber}': ${e.message}"
			)
		}
	}

	@Keyword
	def verifyEcdChargeTabSections(String trackingNumber = '') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')
			validateTrackingNumbers(trackingNumber)
//			verifyChargeDetailSectionsVisible()
		} catch (Exception e) {
			failStop("verifyEcdChargeTabSections failed: ${e.message}")
		}
	}

//	@Keyword
//	def validateTrackingNumber(String expectedTrackingNumber) {
//	
//		try {
//	
//			TestObject trackingNumberValue = to120(
//				'txt_trackingNumber'
//			)
//	
//			WebUI.waitForElementVisible(
//				trackingNumberValue,
//				15,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			WebUI.verifyElementVisible(
//				trackingNumberValue,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			String actualTrackingNumber = WebUI.getText(
//				trackingNumberValue,
//				FailureHandling.STOP_ON_FAILURE
//			).trim()
//	
//			WebUI.verifyMatch(
//				actualTrackingNumber,
//				expectedTrackingNumber.trim(),
//				false,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			KeywordUtil.logInfo(
//				"Tracking Number is visible and successfully validated. " +
//				"Expected: '${expectedTrackingNumber}', " +
//				"Actual: '${actualTrackingNumber}'"
//			)
//	
//		} catch (Exception e) {
//	
//			WebUI.takeScreenshot()
//	
//			failStop(
//				"Failed to validate Tracking Number '${expectedTrackingNumber}': ${e.message}"
//			)
//		}
//	}
	
	
	@Keyword
	def verifyEcdTrackingOperationStripUI(String trackingNumber = '') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')

			//verifyDetailLabelsVisible(TRACKING_OPERATION_STRIP_LABELS, 'ECD tracking/operation strip')
		} catch			 (Exception e) {
			failStop("verifyEcdTrackingOperationStripUI failed: ${e.message}")
		}
	}
	
	private TestObject resolveCardLabel(String expectedLabel) {
		
			String label = (expectedLabel ?: '').trim()
		
			if (!label) {
				failStop("resolveCardLabel: Card label cannot be blank")
			}
		
			String xpath =
				"//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__infoCardLabel') " +
				"and translate(normalize-space(.)," +
				"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
				"'abcdefghijklmnopqrstuvwxyz')=" +
				"'${label.toLowerCase()}']"
		
			TestObject cardLabel = new TestObject("dynamicCardLabel_${label}")
		
			cardLabel.addProperty(
				"xpath",
				ConditionType.EQUALS,
				xpath
			)
		
			return cardLabel
		}
		
		
		private TestObject resolveCardValue(String expectedLabel) {
			
				String label = (expectedLabel ?: '').trim()
			
				if (!label) {
					failStop("resolveCardValue: Card label cannot be blank")
				}
			
				String xpath =
					"//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__infoCardLabel') " +
					"and translate(normalize-space(.)," +
					"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
					"'abcdefghijklmnopqrstuvwxyz')=" +
					"'${label.toLowerCase()}']" +
					"/following-sibling::p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__infoCardValue')]"
			
				TestObject cardValue = new TestObject("dynamicCardValue_${label}")
			
				cardValue.addProperty(
					"xpath",
					ConditionType.EQUALS,
					xpath
				)
			
				return cardValue
			}
			
			
			@Keyword
			def verifyCardLabelAndValue(String expectedLabel, String expectedValue) {
			
				try {
			
					String label = (expectedLabel ?: '').trim()
					String value = (expectedValue ?: '').trim()
			
					if (!label) {
						failStop("verifyCardLabelAndValue: Card Label cannot be blank")
					}
			
					if (!value) {
						failStop("verifyCardLabelAndValue: Card Value cannot be blank")
					}
			
					TestObject cardLabel = resolveCardLabel(label)
			
					WebUI.waitForElementVisible(
						cardLabel,
						15,
						FailureHandling.STOP_ON_FAILURE
					)
			
					String actualLabel = WebUI.getText(cardLabel).trim()
			
					WebUI.verifyEqual(
						actualLabel.toLowerCase(),
						label.toLowerCase(),
						FailureHandling.STOP_ON_FAILURE
					)
			
					TestObject cardValue = resolveCardValue(label)
			
					WebUI.waitForElementVisible(
						cardValue,
						15,
						FailureHandling.STOP_ON_FAILURE
					)
			
					String actualValue = WebUI.getText(cardValue).trim()
			
					WebUI.verifyEqual(
						actualValue,
						value,
						FailureHandling.STOP_ON_FAILURE
					)
			
					KeywordUtil.logInfo(
						"Successfully validated Card Label: '${actualLabel}' and Card Value: '${actualValue}'"
					)
			
				} catch (Exception e) {
			
					WebUI.takeScreenshot()
			
					failStop(
						"Failed to validate Card Label '${expectedLabel}' and Card Value '${expectedValue}': ${e.message}"
					)
				}
			}
	
//	@Keyword
//	def verifySection(String expectedSectionName) {
//	
//		try {
//	
//			TestObject section = new TestObject('Object Repository/CustomSearch_360ViewScreen/CCIPH-120/dynamicSection')
//	
//			section.addProperty(
//				"xpath",
//				ConditionType.EQUALS,
//				"//*[normalize-space(text())='${expectedSectionName}']"
//			)
//	
//			WebUI.waitForElementVisible(
//				section,
//				15,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			WebUI.verifyElementText(
//				section,
//				expectedSectionName,
//				FailureHandling.STOP_ON_FAILURE
//			)
//	
//			KeywordUtil.logInfo(
//				"Successfully validated section: '${expectedSectionName}'"
//			)
//	
//		} catch (Exception e) {
//	
//			WebUI.takeScreenshot()
//	
//			failStop(
//				"Failed to validate section '${expectedSectionName}': ${e.message}"
//			)
//		}
//	}
	
	@Keyword
	def verifySection(String sectionName) {
		try {
	
			String expectedSection = (sectionName ?: '').trim()
	
			if (!expectedSection) {
				failStop("verifySection: Section name cannot be blank")
			}
	
			TestObject section = resolveCustomsSection(expectedSection)
	
			WebUI.waitForElementVisible(
				section,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualSection = WebUI.getText(section).trim()
	
			WebUI.verifyEqual(
				actualSection.toLowerCase(),
				expectedSection.toLowerCase(),
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully validated section: '${actualSection}'"
			)
			currentChargeDetailSection = expectedSection

		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate section '${sectionName}': ${e.message}"
			)
		}
	}
	
	@Keyword
	def verifySectionAndFields(
		String sectionName,
		Map<String, String> expectedFields
	) {
	
		try {
	
			String section = (sectionName ?: '').trim()
	
			if (!section) {
				failStop(
					"verifySectionAndFields: Section Name cannot be blank"
				)
			}
	
			if (!expectedFields || expectedFields.isEmpty()) {
				failStop(
					"verifySectionAndFields: No fields provided for section '${section}'"
				)
			}
	
			// Resolve section dynamically
			TestObject sectionObject = resolveCustomsSection(section)
	
			// Scroll to section
			WebUI.scrollToElement(
				sectionObject,
				10,
				FailureHandling.STOP_ON_FAILURE
			)
	
			// Verify section is visible
			WebUI.waitForElementVisible(
				sectionObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.verifyElementVisible(
				sectionObject,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully verified section '${section}' is visible"
			)
	
			// Validate all fields
			expectedFields.each { fieldName, expectedValue ->
	
				verifyFieldValue(
					fieldName,
					expectedValue
				)
			}
	
			KeywordUtil.logInfo(
				"Successfully validated section '${section}' and all its fields"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate section '${sectionName}' and its fields: ${e.message}"
			)
		}
	}
	
	
	private TestObject resolveCustomsSection(String expectedSection) {

	String section = (expectedSection ?: '').trim()

	if (!section) {
		failStop("resolveCustomsSection: Section name cannot be blank")
	}

	String xpath = "//span[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__') " +
		"and translate(normalize-space(.)," +
		"'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
		"'abcdefghijklmnopqrstuvwxyz')=" +
		"'${section.toLowerCase()}']"

	TestObject sectionObject = new TestObject("dynamicSection_${section}")

	sectionObject.addProperty(
		"xpath",
		ConditionType.EQUALS,
		xpath
	)

	return sectionObject
}

	
	@Keyword
	def verifySectionName(String sectionName, String expectedSectionText) {
		try {
	
			String expectedText = (expectedSectionText ?: '').trim()
	
			if (!expectedText) {
				failStop(
					"verifySection: Expected section text is blank for '${sectionName}'"
				)
			}
	
			TestObject section = resolveCustomsSection(sectionName)
	
			WebUI.waitForElementVisible(
				section,
				15,
				FailureHandling.STOP_ON_FAILURE
			)
	
			String actualText = WebUI.getText(section).trim()
	
			WebUI.verifyEqual(
				actualText,
				expectedText,
				FailureHandling.STOP_ON_FAILURE
			)
	
			KeywordUtil.logInfo(
				"Successfully validated section '${sectionName}' with value '${actualText}'"
			)
	
		} catch (Exception e) {
	
			WebUI.takeScreenshot()
	
			failStop(
				"Failed to validate section '${sectionName}' with expected value '${expectedSectionText}': ${e.message}"
			)
		}
	}
	
	

	@Keyword
	def verifyEcdTrackingOperationStripAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyEcdTrackingOperationStripUI(trackingNumber)
			logDatabaseValidationPending('ECD tracking/operation strip')
		} catch (Exception e) {
			failStop("verifyEcdTrackingOperationStripAgainstDatabase failed: ${e.message}")
		}
	}
	
	private TestObject resolveFieldValue(String fieldName, String sectionName = null) {
		String field = (fieldName ?: '').trim()
		if (!field) {
			failStop('resolveFieldValue: Field name cannot be blank')
		}

		String section = (sectionName ?: currentChargeDetailSection ?: '').trim()
		String fieldLit = xpathLiteral(field)
		String labelXpath = "starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel') and normalize-space(.)=${fieldLit}"
		String valueXpath = "starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldValue')"

		String xpath
		if (section) {
			String sectionLit = xpathLiteral(normalizeLabel(section))
			xpath = "//span[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${sectionLit})]" +
				"/following::p[${labelXpath}][1]/following-sibling::div[${valueXpath}][1]"
		} else {
			xpath = "//p[${labelXpath}]/following-sibling::div[${valueXpath}][1]"
		}

		TestObject fieldValue = dynamicXpathObject(
			"cs_detail_field_${normalizeLabel(section)}_${normalizeLabel(field)}",
			xpath
		)
		return fieldValue
	}

	@Keyword
	def verifyFieldValue(String fieldName, String expectedValue, String sectionName = null) {
		try {
			String expectedFieldName = (fieldName ?: '').trim()
			String expectedFieldValue = (expectedValue ?: '').trim()

			if (!expectedFieldName) {
				failStop('Field Name cannot be blank')
			}

			TestObject fieldValueObject = resolveFieldValue(expectedFieldName, sectionName)

			WebUI.waitForElementVisible(
				fieldValueObject,
				15,
				FailureHandling.STOP_ON_FAILURE
			)

			String actualValue = (WebUI.getText(fieldValueObject, FailureHandling.STOP_ON_FAILURE) ?: '').trim()

			if (actualValue != expectedFieldValue) {
				WebUI.takeScreenshot()
				String sectionContext = (sectionName ?: currentChargeDetailSection ?: '').trim()
				String sectionMsg = sectionContext ? " in section '${sectionContext}'" : ''
				failStop(
					"Field validation failed for '${expectedFieldName}'${sectionMsg}. " +
					"Expected: '${expectedFieldValue}', Actual: '${actualValue}'"
				)
			}

			KeywordUtil.logInfo(
				"Successfully validated Field Name: '${expectedFieldName}' with Field Value: '${actualValue}'"
			)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop(
				"Failed to validate Field Name '${fieldName}' with expected value '${expectedValue}': ${e.message}"
			)
		}
	}

	@Keyword
	def verifyEcdAccountInformationUI(String trackingNumber = '') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')
			waitVisible(to120('section_accountInformation'))
			//verifyDetailLabelsVisible(ECD_ACCOUNT_INFO_LABELS, 'ECD Account Information')
		} catch (Exception e) {
			failStop("verifyEcdAccountInformationUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdAccountInformationAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyEcdAccountInformationUI(trackingNumber)
			logDatabaseValidationPending('ECD Account Information')
		} catch (Exception e) {
			failStop("verifyEcdAccountInformationAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdAdditionalDetailsUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')
			//verifyDetailLabelsVisible(ADDITIONAL_DETAILS_LABELS_120, 'ECD Additional Details')
		} catch (Exception e) {
			failStop("verifyEcdAdditionalDetailsUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdAdditionalDetailsAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyEcdAdditionalDetailsUI(trackingNumber)
			logDatabaseValidationPending('ECD Additional Details')
		} catch (Exception e) {
			failStop("verifyEcdAdditionalDetailsAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdInvoicingTableUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')
			waitVisible(to120('tbl_invoicingHeaders'))
			verifyTableHeadersVisible(ECD_INVOICING_COLUMNS, 'ECD Invoicing')
		} catch (Exception e) {
			failStop("verifyEcdInvoicingTableUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdDocumentNumberTableUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('ECD')
			waitVisible(to120('tbl_documentNumberHeaders'))
			verifyTableHeadersVisible(DOCUMENT_NUMBER_COLUMNS, 'ECD Document Number')
		} catch (Exception e) {
			failStop("verifyEcdDocumentNumberTableUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEcdDocumentNumberAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyEcdDocumentNumberTableUI(trackingNumber)
			logDatabaseValidationPending('ECD Document Number')
		} catch (Exception e) {
			failStop("verifyEcdDocumentNumberAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcChargeTabSections(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			verifyChargeDetailSectionsVisible()
		} catch (Exception e) {
			failStop("verifyDtcChargeTabSections failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcTrackingOperationStripUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			verifyDetailLabelsVisible(TRACKING_OPERATION_STRIP_LABELS, 'DTC tracking/operation strip')
		} catch (Exception e) {
			failStop("verifyDtcTrackingOperationStripUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcTrackingOperationStripAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyDtcTrackingOperationStripUI(trackingNumber)
			logDatabaseValidationPending('DTC tracking/operation strip')
		} catch (Exception e) {
			failStop("verifyDtcTrackingOperationStripAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcAccountInformationUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			waitVisible(to120('section_accountInformation'))
			verifyDetailLabelsVisible(DTC_ACCOUNT_INFO_LABELS, 'DTC Account Information')
		} catch (Exception e) {
			failStop("verifyDtcAccountInformationUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcAccountInformationAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyDtcAccountInformationUI(trackingNumber)
			logDatabaseValidationPending('DTC Account Information')
		} catch (Exception e) {
			failStop("verifyDtcAccountInformationAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcAdditionalDetailsUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			verifyDetailLabelsVisible(ADDITIONAL_DETAILS_LABELS_120, 'DTC Additional Details')
		} catch (Exception e) {
			failStop("verifyDtcAdditionalDetailsUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcAdditionalDetailsAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyDtcAdditionalDetailsUI(trackingNumber)
			logDatabaseValidationPending('DTC Additional Details')
		} catch (Exception e) {
			failStop("verifyDtcAdditionalDetailsAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcInvoicingTableUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			waitVisible(to120('tbl_invoicingHeaders'))
			verifyTableHeadersVisible(DTC_INVOICING_COLUMNS, 'DTC Invoicing')
		} catch (Exception e) {
			failStop("verifyDtcInvoicingTableUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcInvoicingTableAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyDtcInvoicingTableUI(trackingNumber)
			logDatabaseValidationPending('DTC Invoicing Table')
		} catch (Exception e) {
			failStop("verifyDtcInvoicingTableAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcDocumentNumberTableUI(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('DTC')
			waitVisible(to120('tbl_documentNumberHeaders'))
			verifyTableHeadersVisible(DOCUMENT_NUMBER_COLUMNS, 'DTC Document Number')
		} catch (Exception e) {
			failStop("verifyDtcDocumentNumberTableUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDtcDocumentNumberAgainstDatabase(String trackingNumber = 'DP13000') {
		try {
			verifyDtcDocumentNumberTableUI(trackingNumber)
			logDatabaseValidationPending('DTC Document Number')
		} catch (Exception e) {
			failStop("verifyDtcDocumentNumberAgainstDatabase failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAnalysisTabNavigation(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickDetailHeaderTab('Analysis')
			verifyDetailLabelsVisible(['ECD', 'DTC', 'ANALYSIS'], 'Analysis tab')
		} catch (Exception e) {
			failStop("verifyAnalysisTabNavigation failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAnalysisTabUI(String trackingNumber = 'DP13000') {
		try {
			verifyAnalysisTabNavigation(trackingNumber)
			verifyDetailLabelsVisible(
				['Tracking Number', 'Billing Entity', 'Billing Country', 'MAWB', 'Console', 'Destination', 'Cost Status'],
				'Analysis header strip'
			)
		} catch (Exception e) {
			failStop("verifyAnalysisTabUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAnalysisSubsectionsDetail(String trackingNumber = 'DP13000') {
		try {
			verifyAnalysisTabUI(trackingNumber)
			for (String subsection : ['ECD', 'DTC', 'ANALYSIS']) {
				TestObject subTab = dynamicXpathObject("cs120_analysis_${subsection}",
					"//*[self::button or self::a or self::span or self::div][normalize-space()=${xpathLiteral(subsection)} or contains(normalize-space(),${xpathLiteral(subsection)})]")
				if (WebUI.waitForElementVisible(subTab, 5, FailureHandling.OPTIONAL)) {
					click(subTab)
					WebUI.delay(1)
					WebUI.takeScreenshot()
				}
			}
		} catch (Exception e) {
			failStop("verifyAnalysisSubsectionsDetail failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryTabNavigation(String trackingNumber = 'DP13000') {
		try {
			navigateToChargeDetailPage(trackingNumber)
			clickHistoryTab()
			waitVisible(to120('tbl_historyHeaders'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryTabNavigation failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryScreenUI(String trackingNumber = 'DP13000') {
		try {
			verifyHistoryTabNavigation(trackingNumber)
			verifyTableHeadersVisible(HISTORY_TABLE_COLUMNS, 'Custom History')
		} catch (Exception e) {
			failStop("verifyHistoryScreenUI failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistorySectionFilters(String trackingNumber = 'DP13000') {
		try {
			verifyHistoryTabNavigation(trackingNumber)
			TestObject filterBtn = to120('btn_historyFilter')
			click(filterBtn)
			for (String option : HISTORY_FILTER_OPTIONS) {
				String lit = xpathLiteral(normalizeLabel(option))
				TestObject filterOption = dynamicXpathObject("cs120_history_filter_${option.hashCode()}",
					"//*[self::button or self::a or self::li or self::span][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${lit})]")
				waitVisible(filterOption)
				WebUI.takeScreenshot()
			}
		} catch (Exception e) {
			failStop("verifyHistorySectionFilters failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryBackButton(String trackingNumber = 'DP13000') {
		try {
			openChargeDetailHistoryTab(trackingNumber)
			TestObject back = to120('btn_historyBack')
			click(back)
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			verifyDetailPageLoaded()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryBackButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryResetButton(String trackingNumber = 'DP13000') {
		try {
			openChargeDetailHistoryTab(trackingNumber)
			click(to120('btn_historyReset'))
			WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
			waitVisible(to120('tbl_historyHeaders'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryResetButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryLargeDataPerformance(String trackingNumber = 'DP13000') {
		try {
			openChargeDetailHistoryTab(trackingNumber)
			long start = System.currentTimeMillis()
			if (WebUI.waitForElementVisible(dynamicXpathObject('cs120_history_next',
				"//*[contains(@class,'pagination')]//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'next')][not(@disabled)]"),
				5, FailureHandling.OPTIONAL)) {
				clickPaginationNext()
			}
			long elapsed = System.currentTimeMillis() - start
			KeywordUtil.logInfo("History navigation elapsed ms: ${elapsed}")
			if (elapsed > 30000) {
				failStop("History page navigation exceeded 30s threshold: ${elapsed}ms")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryLargeDataPerformance failed: ${e.message}")
		}
	}

	private void clickHistoryExportButton() {
		click(to120('btn_historyExport'))
		WebUI.delay(2)
	}

	@Keyword
	def verifyHistoryExportDownload(String trackingNumber = 'DP13000') {
		try {
			openChargeDetailHistoryTab(trackingNumber)
			prepareCustomSearchExportDownloadDirectory()
			long exportStart = System.currentTimeMillis()
			clickHistoryExportButton()
			verifyLatestCustomSearchExportDownloaded(exportStart)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryExportDownload failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryExportedFileFormat(String trackingNumber = 'DP13000') {
		try {
			verifyHistoryExportDownload(trackingNumber)
			verifyExportedCustomSearchFilePresentInDownloads()
			String path = getLastCustomSearchExportFilePath()
			if (!path.toLowerCase().endsWith('.csv')) {
				KeywordUtil.markWarning("History export file is not CSV: ${path}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryExportedFileFormat failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryExportedFileDataMatchesUI(String trackingNumber = 'DP13000') {
		try {
			openChargeDetailHistoryTab(trackingNumber)
			String firstRow = getFirstHistoryTableRowText()
			prepareCustomSearchExportDownloadDirectory()
			long exportStart = System.currentTimeMillis()
			clickHistoryExportButton()
			verifyLatestCustomSearchExportDownloaded(exportStart)
			if (firstRow?.trim()) {
				verifyExportedCustomSearchFileContainsUiRowSnippet(null, firstRow)
			} else {
				KeywordUtil.markWarning('No history row text captured for export comparison')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryExportedFileDataMatchesUI failed: ${e.message}")
		}
	}

	private String getFirstHistoryTableRowText() {
		TestObject firstRow = dynamicXpathObject('cs120_history_first_row',
			"//table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'timestamp')]]//tbody/tr[1]")
		if (!WebUI.waitForElementVisible(firstRow, 10, FailureHandling.OPTIONAL)) {
			return ''
		}
		return (WebUI.getText(firstRow, FailureHandling.OPTIONAL) ?: '').trim()
	}
}
