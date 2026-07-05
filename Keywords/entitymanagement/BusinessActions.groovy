package entitymanagement

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
import org.openqa.selenium.support.ui.Select
import customsmanagement.ProfileGlobals

/**
 * Entity Management – entity-level invoicing settings (CCIPH-85 Cost-AP).
 * Assumes authenticated session – login is handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX = 'Entity Management/CCIPH-85/'
	private static final int DEFAULT_TIMEOUT = 30
	private static final List<String> COST_AP_CHARGE_TYPES = ['ECD', 'ECS', 'EC3']
	private static final List<String> COST_AP_INVOICING_FIELDS = [
		'Split Entry',
		'Entry Limit',
		'Account',
		'Country',
		'Entity',
		'SAP Company Code'
	]
	private static final List<String> COST_AP_READONLY_FIELDS = ['Country', 'Entity']
	private static final List<String> COST_AP_EDITABLE_FIELDS = [
		'Split Entry',
		'Entry Limit',
		'Account',
		'SAP Company Code'
	]

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
			return '"' + v + '"'
		}
		return "concat('" + v.split("'").join("', \"'\", '") + "')"
	}

	private static String resolveEntitySettingsUrl() {
		String url = (ProfileGlobals.get('ENTITY_SETTINGS_URL') ?: '').trim()
		if (!url) {
			url = 'https://dev-cc.aramex.com/customs-management/entity-settings'
		}
		return url
	}

	private static String invoicingFieldBlockXPath(String labelText) {
		String lit = xpathLiteral(labelText)
		return "//div[contains(@class,'min-w-0')][.//label[normalize-space()=${lit}]]"
	}

	private static TestObject invoicingFieldByLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		String blockXp = invoicingFieldBlockXPath(label)
		TestObject field = new TestObject("em_invoicing_field_${safeId}")
		field.addProperty('xpath', ConditionType.EQUALS,
			"${blockXp}/input[1]" +
			" | ${blockXp}//input[1]" +
			" | ${blockXp}//select[1]" +
			" | ${blockXp}//textarea[1]" +
			" | ${blockXp}//button[@role='combobox'][1]"
		)
		return field
	}

	private static TestObject invoicingFieldLabel(String labelText) {
		String label = (labelText ?: '').trim()
		String safeId = label.replaceAll('[^a-zA-Z0-9]+', '_')
		TestObject labelTo = new TestObject("em_invoicing_label_${safeId}")
		labelTo.addProperty('xpath', ConditionType.EQUALS,
			"${invoicingFieldBlockXPath(label)}//label[normalize-space()=${xpathLiteral(label)}]"
		)
		return labelTo
	}

	private static TestObject chargeTypeTab(String chargeType) {
		String ct = (chargeType ?: '').trim().toUpperCase()
		TestObject tab = new TestObject("em_charge_type_${ct}")
		tab.addProperty('xpath', ConditionType.EQUALS,
			"//button[normalize-space()=${xpathLiteral(ct)}]" +
			" | //div[contains(@role,'tablist')]//button[contains(normalize-space(),${xpathLiteral(ct)})]" +
			" | //*[self::button or self::a][normalize-space()=${xpathLiteral(ct)}]"
		)
		return tab
	}

	private static boolean isFieldReadOnlyOrDisabled(TestObject field) {
		if (!WebUI.waitForElementPresent(field, 8, FailureHandling.OPTIONAL)) {
			return false
		}
		WebElement el = WebUiCommonHelper.findWebElement(field, 10)
		if (el == null) {
			return false
		}
		String tag = (el.getTagName() ?: '').toLowerCase()
		if (tag == 'select') {
			String disabled = el.getAttribute('disabled')
			return disabled != null && !disabled.isEmpty() && !'false'.equalsIgnoreCase(disabled)
		}
		String ro = el.getAttribute('readonly')
		String disabled = el.getAttribute('disabled')
		String ariaReadonly = el.getAttribute('aria-readonly')
		String cls = el.getAttribute('class') ?: ''
		boolean attrReadonly = ro != null && !ro.isEmpty() && !'false'.equalsIgnoreCase(ro)
		boolean attrDisabled = disabled != null && !disabled.isEmpty() && !'false'.equalsIgnoreCase(disabled)
		boolean ariaRo = 'true'.equalsIgnoreCase(ariaReadonly ?: '')
		boolean cursorNotAllowed = cls.contains('cursor-not-allowed')
		return attrReadonly || attrDisabled || ariaRo || cursorNotAllowed || !el.isEnabled()
	}

	private static boolean isFieldEditable(TestObject field) {
		if (!WebUI.waitForElementPresent(field, 8, FailureHandling.OPTIONAL)) {
			return false
		}
		return !isFieldReadOnlyOrDisabled(field)
	}

	private static String readFieldDisplayValue(TestObject field) {
		String value = (WebUI.getAttribute(field, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (value) {
			return value
		}
		return (WebUI.getText(field, FailureHandling.OPTIONAL) ?: '').trim()
	}

	@Keyword
	def navigateToEntitySettings() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			WebUI.navigateToUrl(resolveEntitySettingsUrl())
			waitVisible(to('txt_anchor_EntitySettingsPage'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToEntitySettings failed: ${e.message}")
		}
	}

	@Keyword
	def toggleDropdownValue(String fieldName, String value1, String value2) {
		TestObject dropdown = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/tab_ECD_dropdownField',
			['fieldName': fieldName]
		)

		WebUI.waitForElementVisible(dropdown, 10)
		WebElement element = WebUiCommonHelper.findWebElement(dropdown, 10)
		Select select = new Select(element)

		String currentValue = select.getFirstSelectedOption().getText().trim()
		String targetValue = currentValue.equalsIgnoreCase(value1) ? value2 : value1

		WebUI.selectOptionByLabel(dropdown, targetValue, false)
		KeywordUtil.markPassed("${fieldName} changed from ${currentValue} to ${targetValue}")
	}

	@Keyword
	def verifyEntitySettingsPageLoaded() {
		try {
			waitVisible(to('txt_anchor_EntitySettingsPage'))
			WebUI.verifyElementVisible(to('txt_anchor_EntitySettingsPage'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntitySettingsPageLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def selectEntity(String entityLabel) {
		try {
			String entity = (entityLabel ?: '').trim()
			if (!entity) {
				failStop('selectEntity: entity label is required')
			}
			TestObject ddl = to('ddl_entitySelector')
			waitVisibleClickable(ddl)
			WebUI.selectOptionByLabel(ddl, entity, false)
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectEntity(${entityLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def clickEntitySelectionContinue() {
		try {
			click(to('btn_entityContinue'))
			WebUI.delay(1)
		} catch (Exception e) {
			failStop("clickEntitySelectionContinue failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntitySelectedAndChargeTypesEnabled() {
		try {
			verifyEntitySettingsPageLoaded()
			waitVisible(to('section_chargeTypes'))
			WebUI.verifyElementVisible(to('section_chargeTypes'), FailureHandling.STOP_ON_FAILURE)
			COST_AP_CHARGE_TYPES.each { String ct ->
				TestObject tab = chargeTypeTab(ct)
				if (!WebUI.waitForElementPresent(tab, 8, FailureHandling.OPTIONAL)) {
					failStop("Charge type '${ct}' is not enabled after entity selection")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntitySelectedAndChargeTypesEnabled failed: ${e.message}")
		}
	}

	@Keyword
	def selectEntityContinueAndVerifyChargeTypesEnabled(String entityLabel) {
		try {
			verifyEntitySettingsPageLoaded()
			selectEntity(entityLabel)
			clickEntitySelectionContinue()
			waitVisible(to('section_chargeTypes'))
			WebUI.verifyElementVisible(to('section_chargeTypes'), FailureHandling.STOP_ON_FAILURE)
			COST_AP_CHARGE_TYPES.each { String ct ->
				TestObject tab = chargeTypeTab(ct)
				if (!WebUI.waitForElementPresent(tab, 8, FailureHandling.OPTIONAL)) {
					failStop("Charge type '${ct}' is not enabled after entity selection and Continue")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectEntityContinueAndVerifyChargeTypesEnabled failed: ${e.message}")
		}
	}

	@Keyword
	def clickCostApSection() {
		try {
			click(to('tab_CostAp'))
			WebUI.delay(1)
		} catch (Exception e) {
			failStop("clickCostApSection failed: ${e.message}")
		}
	}

	@Keyword
	def selectChargeType(String chargeType) {
		try {
			String ct = (chargeType ?: '').trim().toUpperCase()
			if (!ct) {
				failStop('selectChargeType: charge type is required')
			}
			TestObject tab = chargeTypeTab(ct)
			click(tab)
			WebUI.delay(1)
		} catch (Exception e) {
			failStop("selectChargeType(${chargeType}) failed: ${e.message}")
		}
	}

	@Keyword
	def openEntitySettingsCostAp(String entityLabel) {
		try {
			verifyEntitySettingsPageLoaded()
			selectEntity(entityLabel)
		} catch (Exception e) {
			failStop("openEntitySettingsCostAp failed: ${e.message}")
		}
	}

	@Keyword
	def openEntitySettingsCostApChargeType(String entityLabel, String chargeType) {
		try {
			openEntitySettingsCostAp(entityLabel)
			selectChargeType(chargeType)
		} catch (Exception e) {
			failStop("openEntitySettingsCostApChargeType failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCostApChargeTypesDisplayed(List<String> expectedChargeTypes = null) {
		try {
			List<String> expected = (expectedChargeTypes ?: COST_AP_CHARGE_TYPES).collect { it.trim().toUpperCase() }.findAll { it }
			expected.each { String ct ->
				TestObject tab = chargeTypeTab(ct)
				waitVisible(tab)
				WebUI.verifyElementVisible(tab, FailureHandling.STOP_ON_FAILURE)
			}
			KeywordUtil.logInfo("Cost-AP charge types visible: ${expected}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCostApChargeTypesDisplayed failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingSettingsVisibleForAllCostChargeTypes() {
		try {
			COST_AP_CHARGE_TYPES.each { String ct ->
				selectChargeType(ct)
				COST_AP_INVOICING_FIELDS.each { String field ->
					verifyInvoicingFieldVisible(field)
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvoicingSettingsVisibleForAllCostChargeTypes failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingFieldVisible(String fieldLabel) {
		try {
			TestObject label = invoicingFieldLabel(fieldLabel)
			waitVisible(label)
			WebUI.verifyElementVisible(label, FailureHandling.STOP_ON_FAILURE)
			TestObject field = invoicingFieldByLabel(fieldLabel)
			waitVisible(field)
			WebUI.verifyElementVisible(field, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvoicingFieldVisible(${fieldLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingFieldEditable(String fieldLabel) {
		try {
			verifyInvoicingFieldVisible(fieldLabel)
			TestObject field = invoicingFieldByLabel(fieldLabel)
			if (!isFieldEditable(field)) {
				failStop("Expected field '${fieldLabel}' to be editable")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvoicingFieldEditable(${fieldLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingFieldReadOnly(String fieldLabel) {
		try {
			verifyInvoicingFieldVisible(fieldLabel)
			TestObject field = invoicingFieldByLabel(fieldLabel)
			if (!isFieldReadOnlyOrDisabled(field)) {
				failStop("Expected field '${fieldLabel}' to be read-only/disabled")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvoicingFieldReadOnly(${fieldLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingFieldForChargeType(String entityLabel, String chargeType, String fieldLabel, boolean editable) {
		try {
			openEntitySettingsCostApChargeType(entityLabel, chargeType)
			if (editable) {
				verifyInvoicingFieldEditable(fieldLabel)
			} else {
				verifyInvoicingFieldReadOnly(fieldLabel)
			}
		} catch (Exception e) {
			failStop("verifyInvoicingFieldForChargeType failed: ${e.message}")
		}
	}

	@Keyword
	def clickProfileTab(String tabName) {
		TestObject tab = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/btn_CostTab',
			['tabName': tabName]
		)

		WebUI.waitForElementClickable(tab, 10)
		WebUI.click(tab)
		KeywordUtil.logInfo("${tabName} tab clicked successfully")
	}

	@Keyword
	def verifyFieldIsReadOnly(String fieldName) {
		TestObject field = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/tab_ECD_inputField',
			['fieldName': fieldName]
		)

		WebUI.verifyElementPresent(field, 10)

		String readOnly = WebUI.getAttribute(field, 'readonly')
		String disabled = WebUI.getAttribute(field, 'disabled')

		KeywordUtil.logInfo("${fieldName} readonly attribute: ${readOnly}")
		KeywordUtil.logInfo("${fieldName} disabled attribute: ${disabled}")

		if (readOnly == null && disabled == null) {
			KeywordUtil.markFailedAndStop("${fieldName} field is not read-only")
		}

		KeywordUtil.markPassed("${fieldName} field verified as ReadOnly")
	}

	@Keyword
	def verifyDropdownField(String fieldName, List<String> expectedOptions, String expectedSelectedValue = null) {
		TestObject dropdown = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/tab_ECD_dropdownField',
			['fieldName': fieldName]
		)

		WebUI.verifyElementPresent(dropdown, 10)

		WebElement element = WebUiCommonHelper.findWebElement(dropdown, 10)
		Select select = new Select(element)

		List<String> actualOptions = select.getOptions().collect { it.getText().trim() }
		KeywordUtil.logInfo("Actual Options: ${actualOptions}")

		expectedOptions.each { option ->
			if (!actualOptions.contains(option)) {
				KeywordUtil.markFailedAndStop(
					"Option '${option}' not found in dropdown '${fieldName}'. Available options: ${actualOptions}"
				)
			}
		}

		if (expectedSelectedValue) {
			String actualSelectedValue = select.getFirstSelectedOption().getText().trim()
			WebUI.verifyMatch(actualSelectedValue, expectedSelectedValue, false)
			KeywordUtil.logInfo("Selected Value Verified: ${actualSelectedValue}")
		}

		KeywordUtil.markPassed("Dropdown '${fieldName}' validated successfully")
	}

	@Keyword
	def verifyInputField(String fieldName, String expectedValue) {
		TestObject inputField = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/tab_ECD_inputField',
			['fieldName': fieldName]
		)

		WebUI.verifyElementPresent(inputField, 10)

		String actualValue = (WebUI.getAttribute(inputField, 'value') ?: '').trim()
		WebUI.verifyMatch(actualValue, expectedValue, false)

		KeywordUtil.markPassed("Input field '${fieldName}' validated successfully. Value: ${actualValue}")
	}

	@Keyword
	def clickSaveButton() {
		TestObject saveButton = findTestObject('Object Repository/Entity Management/CCIPH-85/btn_saveEntityConfiguration')
		WebUI.waitForElementClickable(saveButton, 10)
		WebUI.click(saveButton)
		KeywordUtil.markPassed('Save button clicked successfully')
	}

	@Keyword
	def saveButtonPopUp() {
		TestObject popupButton = findTestObject('Object Repository/Entity Management/CCIPH-85/btn_SavePopUp')
		WebUI.waitForElementClickable(popupButton, 10)
		WebUI.click(popupButton)
		KeywordUtil.markPassed('Success. Settings saved successfully!')
	}

	@Keyword
	def updateInputField(String fieldName, String value) {
		TestObject field = findTestObject(
			'Object Repository/Entity Management/CCIPH-85/tab_ECD_inputField',
			['fieldName': fieldName]
		)

		WebUI.waitForElementVisible(field, 10)
		WebUI.clearText(field)
		WebUI.setText(field, value)
		KeywordUtil.markPassed("${fieldName} updated with value ${value}")
	}

	@Keyword
	def verifyAllCostApInvoicingFieldsForChargeType(String entityLabel, String chargeType) {
		try {
			openEntitySettingsCostApChargeType(entityLabel, chargeType)
			COST_AP_READONLY_FIELDS.each { verifyInvoicingFieldReadOnly(it) }
			COST_AP_EDITABLE_FIELDS.each { verifyInvoicingFieldEditable(it) }
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyAllCostApInvoicingFieldsForChargeType failed: ${e.message}")
		}
	}

	@Keyword
	def enterEntryLimitValue(String value) {
		try {
			TestObject field = invoicingFieldByLabel('Entry Limit')
			waitVisibleClickable(field)
			WebUI.clearText(field, FailureHandling.OPTIONAL)
			WebUI.setText(field, (value ?: '').trim())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterEntryLimitValue failed: ${e.message}")
		}
	}

	@Keyword
	def enterSapCompanyCodeValue(String value) {
		try {
			TestObject field = invoicingFieldByLabel('SAP Company Code')
			waitVisibleClickable(field)
			WebUI.clearText(field, FailureHandling.OPTIONAL)
			WebUI.setText(field, (value ?: '').trim())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterSapCompanyCodeValue failed: ${e.message}")
		}
	}

	@Keyword
	def clickSaveEntityInvoicingConfiguration() {
		try {
			click(to('btn_saveEntityConfiguration'))
			WebUI.delay(2)
		} catch (Exception e) {
			failStop("clickSaveEntityInvoicingConfiguration failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntityConfigurationSavedSuccessfully(String expectedMessage = null) {
		try {
			TestObject msg = to('msg_configurationSavedSuccess')
			waitVisible(msg)
			if ((expectedMessage ?: '').trim()) {
				String actual = (WebUI.getText(msg, FailureHandling.OPTIONAL) ?: '').trim()
				if (!actual.toLowerCase().contains(expectedMessage.trim().toLowerCase())) {
					failStop("Expected save confirmation containing '${expectedMessage}', got '${actual}'")
				}
			}
			WebUI.verifyElementVisible(msg, FailureHandling.STOP_ON_FAILURE)
			dismissConfigurationSavedMessageIfPresent()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityConfigurationSavedSuccessfully failed: ${e.message}")
		}
	}

	private static void dismissConfigurationSavedMessageIfPresent() {
		try {
			if (WebUI.waitForElementPresent(to('btn_configurationSavedOk'), 5, FailureHandling.OPTIONAL)) {
				click(to('btn_configurationSavedOk'))
			}
		} catch (Exception ignored) {
		}
	}

	@Keyword
	def saveEntityInvoicingConfigurationAndVerify() {
		try {
			clickSaveEntityInvoicingConfiguration()
			verifyEntityConfigurationSavedSuccessfully()
		} catch (Exception e) {
			failStop("saveEntityInvoicingConfigurationAndVerify failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntitySelectionSwitchWorks(String firstEntity, String secondEntity) {
		try {
			verifyEntitySettingsPageLoaded()
			selectEntity(firstEntity)
			verifyEntitySelectedAndChargeTypesEnabled()
			selectEntity(secondEntity)
			verifyEntitySelectedAndChargeTypesEnabled()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntitySelectionSwitchWorks failed: ${e.message}")
		}
	}

	@Keyword
	def verifyChargeTypeIndependence(String entityLabel, String primaryChargeType, String otherChargeType, String fieldLabel) {
		try {
			openEntitySettingsCostApChargeType(entityLabel, primaryChargeType)
			String beforeOther = readFieldDisplayValue(invoicingFieldByLabel(fieldLabel))
			String uniqueValue = String.valueOf(System.currentTimeMillis() % 100000)
			if ('Entry Limit'.equalsIgnoreCase(fieldLabel)) {
				enterEntryLimitValue(uniqueValue)
			} else if ('SAP Company Code'.equalsIgnoreCase(fieldLabel)) {
				enterSapCompanyCodeValue(uniqueValue)
			} else {
				failStop("verifyChargeTypeIndependence: unsupported field '${fieldLabel}' for change")
			}
			clickSaveEntityInvoicingConfiguration()
			verifyEntityConfigurationSavedSuccessfully()
			selectChargeType(otherChargeType)
			String afterOther = readFieldDisplayValue(invoicingFieldByLabel(fieldLabel))
			if (beforeOther && afterOther && beforeOther != afterOther) {
				failStop("Charge type '${otherChargeType}' value changed after saving '${primaryChargeType}' only")
			}
			selectChargeType(primaryChargeType)
			String primaryValue = readFieldDisplayValue(invoicingFieldByLabel(fieldLabel))
			if (!primaryValue.contains(uniqueValue)) {
				failStop("Expected '${primaryChargeType}' to retain saved value '${uniqueValue}', got '${primaryValue}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyChargeTypeIndependence failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntityInvoicingViewOnlyAccess(String entityLabel, String chargeType = 'ECD') {
		try {
			openEntitySettingsCostApChargeType(entityLabel, chargeType)
			COST_AP_EDITABLE_FIELDS.each { String field ->
				TestObject fieldObj = invoicingFieldByLabel(field)
				if (WebUI.waitForElementPresent(fieldObj, 8, FailureHandling.OPTIONAL) && isFieldEditable(fieldObj)) {
					failStop("View-only user should not have editable field '${field}'")
				}
			}
			TestObject saveBtn = to('btn_saveEntityConfiguration')
			if (WebUI.waitForElementPresent(saveBtn, 8, FailureHandling.OPTIONAL)) {
				WebElement el = WebUiCommonHelper.findWebElement(saveBtn, 10)
				if (el != null && el.isEnabled()) {
					String disabled = el.getAttribute('disabled')
					boolean attrDisabled = disabled != null && !disabled.isEmpty() && !'false'.equalsIgnoreCase(disabled)
					if (!attrDisabled) {
						failStop('View-only user should not have Save enabled')
					}
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityInvoicingViewOnlyAccess failed: ${e.message}")
		}
	}

	@Keyword
	def verifyInvoicingConfigurationVisibleForAnotherEntity(String primaryEntity, String alternateEntity) {
		try {
			openEntitySettingsCostAp(alternateEntity)
			verifyCostApChargeTypesDisplayed()
			verifyInvoicingSettingsVisibleForAllCostChargeTypes()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyInvoicingConfigurationVisibleForAnotherEntity failed: ${e.message}")
		}
	}
}
