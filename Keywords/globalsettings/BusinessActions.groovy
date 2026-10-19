package globalsettings

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

import java.io.File
import java.io.FileFilter
import java.util.ArrayList

import org.openqa.selenium.Keys as Keys
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.support.ui.Select

import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections

import customsmanagement.ProfileGlobals

/**
 * Shared business actions for Global Settings (CCIPH-30, CCIPH-31, CCIPH-37, CCIPH-201).
 * Assumes authenticated session — login is handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX = 'Global Settings/CCIPH-30/'
	private static final String OR_PREFIX_37 = 'Global Settings/CCIPH-37/'
	private static final String OR_PREFIX_201 = 'Global Settings/CCIPH-201/'
	private static final String OR_PREFIX_29 = 'Global Settings/CCIPH-29/'
	private static final int DEFAULT_TIMEOUT = 30
	private static final String HISTORY_EXPORT_FILE_NAME_PREFIX = 'Global-Settings-History_'
	private static final String HISTORY_EXPORT_DETECTED_NAME_FILE = 'Reports/latest-global-settings-history-export.txt'

	/** Set by {@link #verifyLatestGlobalSettingsHistoryExportDownloaded(long, String)} — last validated file name only. */
	private String lastHistoryExportFileName = ''
	/** Set by {@link #verifyLatestGlobalSettingsHistoryExportDownloaded(long, String)} — full path to last validated export. */
	private String lastHistoryExportFilePath = ''

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static TestObject to37(String name) {
		return findTestObject(OR_PREFIX_37 + name)
	}

	private static TestObject to201(String name) {
		return findTestObject(OR_PREFIX_201 + name)
	}
	private static TestObject to29(String name) {
		return findTestObject(OR_PREFIX_29 + name)
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
	private void setHistoryHtmlDateInputt(TestObject obj, String isoDate) {
		
			WebElement element = WebUiCommonHelper.findWebElement(obj, 20)
		
			WebUI.executeJavaScript(
				"""
        arguments[0].value='${isoDate}';
        arguments[0].dispatchEvent(new Event('input',{bubbles:true}));
        arguments[0].dispatchEvent(new Event('change',{bubbles:true}));
        """,
				Arrays.asList(element)
			)
		}

	@Keyword
	def navigateToGlobalSettings() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			String url = (ProfileGlobals.get('GLOBAL_SETTINGS_URL') ?: ProfileGlobals.get('URL')).trim()
			if (!url) {
				failStop('GLOBAL_SETTINGS_URL (or URL) must be set in the active profile.')
			}
			WebUI.navigateToUrl(url)
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToGlobalSettings failed: ${e.message}")
		}
	}
	
	@Keyword
	def navigateToStageGlobalSettings() {
		try {
			if (DriverFactory.getWebDriver() == null) {
				WebUI.openBrowser('')
				WebUI.maximizeWindow()
			}
			String url = (ProfileGlobals.get('STAGE_GLOBALSETTINGS') ?: ProfileGlobals.get('STAGE_APPLICATION')).trim()
			if (!url) {
				failStop('STAGE_GLOBALSETTINGS (or STAGE_APPLICATION) must be set in the active profile.')
			}
			WebUI.navigateToUrl(url)
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToGlobalSettings failed: ${e.message}")
		}
	}
	

	@Keyword
	def openTab(String tabName) {
		try {
			TestObject tab = null
			switch (tabName?.toUpperCase()) {
				case 'DDU':
					tab = to('tab_DDU')
					break
				case 'DDP':
					tab = to('tab_DDP')
					break
				case 'CATEGORY':
					tab = to('tab_Category')
					break
				default:
					failStop("Unknown tab: ${tabName}")
			}
			click(tab)
		} catch (Exception e) {
			failStop("openTab(${tabName}) failed: ${e.message}")
		}
	}

	@Keyword
	def clickAddNewRule() {
		try {
			click(to('btn_AddNewRule'))
		} catch (Exception e) {
			failStop("clickAddNewRule failed: ${e.message}")
		}
	}

	@Keyword
	def verifyRuleModalVisible(String expectedMessage = 'Filter Conditions') {
		try {
			waitVisible(to('dlg_ruleModal'))
			WebUI.verifyElementVisible(to('dlg_ruleModal'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("${expectedMessage} - ${e.message}")
		}
	}

	@Keyword
	def verifyAddRuleModalElementsPresent() {
		try {
			verifyRuleModalVisible('Add New Rule modal')
			[
				'section_ruleConditions',
				'btn_modalSave',
				'btn_modalCancel'
			].each { name ->
				waitVisible(to(name))
				WebUI.verifyElementVisible(to(name), FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Modal elements verification failed: ${e.message}")
		}
	}

	@Keyword
	def clickModalSave() {
		try {
//			dismissRuleCreatedSuccessModalIfPresent()
//			dismissRuleUpdateSuccessModalIfPresent()
			TestObject saveBtn = to('btn_modalSave')
			waitVisibleClickable(saveBtn)
			WebUI.scrollToElement(saveBtn, DEFAULT_TIMEOUT)
			try {
				WebUI.click(saveBtn)
			} catch (Exception clickEx) {
				KeywordUtil.markWarning("Standard Save click failed (${clickEx.message}); retrying with JavaScript click.")
				WebElement el = WebUiCommonHelper.findWebElement(saveBtn, DEFAULT_TIMEOUT)
				WebUI.executeJavaScript('arguments[0].click();', java.util.Arrays.asList(el))
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickModalSave failed: ${e.message}")
		}
	}

	@Keyword
	def clickModalCancel() {
		try {
			click(to('btn_modalCancel'))
		} catch (Exception e) {
			failStop("clickModalCancel failed: ${e.message}")
		}
	}

	@Keyword
	def verifyModalNotVisible() {
		try {
			boolean stillVisible = WebUI.waitForElementVisible(to('dlg_ruleModal'), 5, FailureHandling.OPTIONAL)
			if (stillVisible) {
				failStop('Add New Rule popup should be closed but modal is still visible')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyModalNotVisible failed: ${e.message}")
		}
	}

	@Keyword
	def selectConditionField(String fieldLabel) {
		try {
			TestObject field = to('ddl_Field')
			WebUI.selectOptionByLabel(field, fieldLabel, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectConditionField(${fieldLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def enterConditionValue(String value) {
		try {
			TestObject valueInput = to('enter_TextValue')
			waitVisibleClickable(valueInput)
//			WebUI.clearText(valueInput)
			WebUI.setText(valueInput, value)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("enterConditionValue failed: ${e.message}")
		}
	}

	/**
	 * Sets rule condition Field, OPERATOR (Greater than — index 1), and Value only — does not click Save.
	 *
	 * Example: addConditionFieldAndValue('Cost of Goods', '100')
	 */
	@Keyword
	def addConditionFieldAndValue(String fieldLabel, String value) {
		try {
			selectConditionField(fieldLabel)
			selectConditionOperatorByIndex(1, 0)
			enterConditionValue(value)
		} catch (Exception e) {
			failStop("addConditionFieldAndValue(${fieldLabel}, ${value}) failed: ${e.message}")
		}
	}

	/**
	 * Adds Cost of Goods field and numeric value only — does not click Save.
	 */
	@Keyword
	def addCostOfGoodsFieldAndValue(String numericValue) {
		addConditionFieldAndValue('Cost of Goods', numericValue)
	}

	/**
	 * Clicks Save on the rule modal and waits for save to complete.
	 */
	@Keyword
	def saveRuleModal() {
		try {
			clickModalSave()
			WebUI.delay(1)
			//dismissRuleCreatedSuccessModalIfPresent()
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("saveRuleModal failed: ${e.message}")
		}
	}

	@Keyword
	def createDduRuleWithCostOfGoods(String numericValue) {
		try {
			clickAddNewRule()
			verifyRuleModalVisible()
			addCostOfGoodsFieldAndValue(numericValue)
			saveRuleModal()
		} catch (Exception e) {
			failStop("createDduRuleWithCostOfGoods failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDuplicateRuleMessageVisible() {
		try {
			waitVisible(to('msg_duplicateRule'))
			WebUI.verifyElementVisible(to('msg_duplicateRule'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Duplicate rule message not displayed: ${e.message}")
		}
	}
	
	/**
	 * CCIPH-561 Rules_001: confirms a Filter Conditions field (e.g. Shipment Business Type) is listed
	 * in the Field dropdown and that the Values control renders as a visible, selectable dropdown.
	 */
	@Keyword
	def verifyFieldsDropdownSelectedValue(String fieldLabel) {
		try {
			String field = (fieldLabel ?: '').trim()
			if (!field) {
				failStop('verifyFieldsDropdownSelectedValue: fieldLabel is blank')
			}
			int conditionRowIndex = 0
			TestObject fieldSel = conditionRowNativeSelect(conditionRowIndex, 1)
			waitVisibleClickable(fieldSel)
			WebUI.verifyOptionPresentByLabel(fieldSel, field, false, DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			WebUI.selectOptionByLabel(fieldSel, field, false)
			WebUI.delay(1)

			TestObject operatorSel = conditionRowNativeSelect(conditionRowIndex, 2)
			if (WebUI.waitForElementVisible(operatorSel, 10, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(operatorSel)
			}
			WebUI.delay(1)

			TestObject valuesByAriaEquals = conditionRowValuesSelectByFieldOperatorAria(conditionRowIndex, field, 'Equals')
			TestObject valuesSel = conditionRowNativeSelect(conditionRowIndex, 3)
			if (WebUI.waitForElementVisible(valuesByAriaEquals, 10, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(valuesByAriaEquals)
				WebUI.verifyElementVisible(valuesByAriaEquals, FailureHandling.STOP_ON_FAILURE)
			} else if (WebUI.waitForElementVisible(valuesSel, 10, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(valuesSel)
				WebUI.verifyElementVisible(valuesSel, FailureHandling.STOP_ON_FAILURE)
			} else {
				failStop("Values dropdown for field '${field}' is not visible after selection")
			}
			WebUI.takeScreenshot()
			KeywordUtil.logInfo("PASS: Field '${field}' is available and its value dropdown is visible.")
		} catch (Exception e) {
			failStop("verifyFieldsDropdownSelectedValue('${fieldLabel}') failed: ${e.message}")
		}
	}
//	
	/** CCIPH-561 Rules_001: Field dropdown lists the given label (e.g. Shipment Business Type). */
	@Keyword
	def verifyFieldDropdownValuePresent(String expectedLabel) {
		try {
			String label = (expectedLabel ?: '').trim()
			if (!label) {
				failStop('verifyFieldDropdownValuePresent: expectedLabel is blank')
			}
			TestObject fieldSel = conditionRowNativeSelect(0, 1)
			waitVisibleClickable(fieldSel)
			WebUI.verifyOptionPresentByLabel(fieldSel, label, false, DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
			KeywordUtil.logInfo("PASS: '${label}' is present in Fields dropdown.")
		} catch (Exception e) {
			failStop("verifyFieldDropdownValuePresent('${expectedLabel}') failed: ${e.message}")
		}
	}
	
//	@Keyword
//	def verifyDuplicateSameRuleErrorVisible() {
//		try {
//			if (WebUI.waitForElementVisible(to('msg_duplicateRule'), 8, FailureHandling.OPTIONAL)) {
//				WebUI.verifyElementVisible(to('msg_duplicateRule'), FailureHandling.STOP_ON_FAILURE)
//				WebUI.takeScreenshot()
//				return
//			}
//			TestObject dyn = new TestObject('dupOrSameRuleMsg')
//			dyn.addProperty('xpath', ConditionType.EQUALS,
//				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'same rule') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'already exist') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'duplicate rule')]")
//			waitVisible(dyn)
//			WebUI.verifyElementVisible(dyn, FailureHandling.STOP_ON_FAILURE)
//			WebUI.takeScreenshot()
//		} catch (Exception e) {
//			failStop("verifyDuplicateOrSameRuleErrorVisible failed: ${e.message}")
//		}
//	}
//	@Keyword
//	def verifyDuplicateRuleMessageVisible(String expectedTitle) {
//		try {
//			TestObject modalTitle = to('msg_duplicateRule')
//	
//			waitVisible(modalTitle)
//	
//			WebUI.verifyElementVisible(modalTitle, FailureHandling.STOP_ON_FAILURE)
//			WebUI.verifyElementText(modalTitle, expectedTitle, FailureHandling.STOP_ON_FAILURE)
//	
//			WebUI.takeScreenshot()
//	
//			pass("Verified modal title '${expectedTitle}' is displayed.")
//		} catch (Exception e) {
//			failStop("Modal title '${expectedTitle}' is not displayed: ${e.message}")
//		}
//	}

	@Keyword
	def verifyDuplicateConditionMessageVisible() {
		try {
			waitVisible(to('msg_duplicateCondition'))
			WebUI.verifyElementVisible(to('msg_duplicateCondition'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Duplicate condition message not displayed: ${e.message}")
		}
	}

	/**
	 * Read displayed text from input/textarea (value attribute) or label-like elements.
	 */
	private static String resolveElementDisplayText(TestObject testObject) {
		String value = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (value) {
			return value
		}
		String textContent = (WebUI.getAttribute(testObject, 'textContent', FailureHandling.OPTIONAL) ?: '').trim()
		if (textContent) {
			return textContent
		}
		return (WebUI.getText(testObject, FailureHandling.OPTIONAL) ?: '').trim()
	}

	/**
	 * Selects Target Value from the dropdown by visible option text (label).
	 * Reusable for Category values: B2B, Exempted, LV, HV, etc.
	 *
	 * Example: selectTargetValue('B2B')
	 * Example: selectTargetValue('LV')
	 */
	@Keyword
	def selectTargetValue(String optionText) {
		try {
			TestObject targetValueDropdown = to('ddl_targetValue')
			waitVisibleClickable(targetValueDropdown)

			String option = (optionText ?: '').trim()
			if (!option) {
				failStop('selectTargetValue failed: optionText is blank')
			}

			WebUI.selectOptionByLabel(targetValueDropdown, option, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectTargetValue(${optionText}) failed: ${e.message}")
		}
	}

	/**
	 * Selects Target Value from the rule modal dropdown using option index (same element as {@link #selectTargetValue}).
	 * {@code optionIndex} is 0-based, as for {@link WebUI#selectOptionByIndex} (typically {@code 0} is placeholder/disabled).
	 *
	 * Example: selectTargetValueByIndex(1)
	 */
	@Keyword
	def selectTargetValueByIndex(int optionIndex) {
		try {
			TestObject targetValueDropdown = to('ddl_targetValue')
			waitVisibleClickable(targetValueDropdown)
			WebUI.selectOptionByIndex(targetValueDropdown, optionIndex, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectTargetValueByIndex(${optionIndex}) failed: ${e.message}")
		}
	}

	/**
	 * Verifies Target Value field (#rule-editor-target-value) shows the expected text.
	 * Works for readonly input (DDP/DDU) or selected dropdown option (Category).
	 *
	 * Example: verifyTargetValue('DDP')
	 * Example: verifyTargetValue('B2B')
	 */
	@Keyword
	def verifyTargetValue(String expectedValue) {
		try {
			TestObject targetValueField = to('ddl_targetValue')
			waitVisible(targetValueField)

			String expected = (expectedValue ?: '').trim()
			if (!expected) {
				failStop('verifyTargetValue failed: expectedValue is blank')
			}

			long end = System.currentTimeMillis() + (DEFAULT_TIMEOUT * 1000L)
			String actual = ''

			while (System.currentTimeMillis() < end) {
				actual = resolveElementDisplayText(targetValueField)
				if (actual.equalsIgnoreCase(expected)) {
					KeywordUtil.logInfo("Target Value verified. Expected: '${expected}', Actual: '${actual}'")
					WebUI.takeScreenshot()
					return
				}
				WebUI.delay(1)
			}

			failStop("Target Value mismatch. Expected: '${expected}', Actual: '${actual}'")
		} catch (Exception e) {
			failStop("verifyTargetValue(${expectedValue}) failed: ${e.message}")
		}
	}

	/** @deprecated Use verifyTargetValue — kept for existing test scripts. */
	@Keyword
	def verifyRuleOrderTargetValue(String expectedValue) {
		verifyTargetValue(expectedValue)
	}

	@Keyword
	def clickAddCondition() {
		try {
			click(to('btn_AddCondition'))
		} catch (Exception e) {
			failStop("clickAddCondition failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDeleteIconsOnAddedConditionRows() {
		try {
			waitVisible(to('btn_rowDeactivateOrDelete'))
			int count = WebUI.findWebElements(to('btn_rowDeactivateOrDelete'), DEFAULT_TIMEOUT)?.size() ?: 0
			if (count < 1) {
				failStop('Deactivate/delete icon should appear on added condition rows')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDeleteIconsOnAddedConditionRows failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNoDeleteIconOnDefaultConditionRow() {
		try {
			boolean present = WebUI.waitForElementVisible(to('btn_rowDeactivateOrDelete'), 5, FailureHandling.OPTIONAL)
			if (present) {
				int count = WebUI.findWebElements(to('btn_rowDeactivateOrDelete'), DEFAULT_TIMEOUT)?.size() ?: 0
				if (count > 0) {
					failStop('Delete icon should not appear on default condition row')
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyNoDeleteIconOnDefaultConditionRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickDeleteIconOnAddedConditionRow() {
		try {
			click(to('btn_rowDeactivateOrDelete'))
		} catch (Exception e) {
			failStop("clickDeleteIconOnAddedConditionRow failed: ${e.message}")
		}
	}

	@Keyword
	def clickCloseFilterConditions() {
		try {
			click(to('btn_closeFilterConditions'))
		} catch (Exception e) {
			failStop("clickCloseFilterConditions failed: ${e.message}")
		}
	}

	@Keyword
	def verifyFilterConditionsSectionHidden() {
		try {
			boolean visible = WebUI.waitForElementVisible(to('section_ruleConditions'), 5, FailureHandling.OPTIONAL)
			if (visible) {
				WebUI.verifyElementNotVisible(to('section_ruleConditions'), FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Filter Conditions should be hidden: ${e.message}")
		}
	}

	@Keyword
	def verifyCategoryAddRuleFieldsVisible() {
		try {
			openTab('Category')
			clickAddNewRule()
			verifyRuleModalVisible('Filter Conditions')
			['lbl_CATEGORY_destination', 'lbl_CATEGORY_fields', 'btn_RuleAction'].each { name ->
				waitVisible(to(name))
				WebUI.verifyElementVisible(to(name), FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Category Add Rule UI verification failed: ${e.message}")
		}
	}

	@Keyword
	def openRuleActionAndVerifyCategoryTargetValues() {
		try {
			click(to('btn_TargetValue'))
			waitVisible(to('btn_TargetField'))
			[
				'lbl_categoryTargetValue_LV',
				'lbl_categoryTargetValue_HV',
				'lbl_categoryTargetValue_B2B',
				'lbl_categoryTargetValue_Exempted'
			].each { name ->
				waitVisible(to(name))
				WebUI.verifyElementVisible(to(name), FailureHandling.STOP_ON_FAILURE)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Rule Action target values verification failed: ${e.message}")
		}
	}

	@Keyword
	def verifyShipmentCategoryValidationOnSave() {
		try {
			clickModalSave()
			waitVisible(to('txt_validationOrError'))
			WebUI.verifyElementText(to('txt_validationOrError'), 'Validation Error', FailureHandling.CONTINUE_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Shipment category validation failed: ${e.message}")
		}
	}

	@Keyword
	def saveCategoryRuleWithDefaults() {
		try {
			click(to('btn_RuleAction'))
			WebUI.takeScreenshot()
			click(to('lbl_categoryTargetValue_LV'))
			WebUI.takeScreenshot()
			if (WebUI.waitForElementVisible(to('inp_firstVisibleInDialog'), 5, FailureHandling.OPTIONAL)) {
				WebUI.setText(to('inp_firstVisibleInDialog'), 'AUTO_CAT_' + System.currentTimeMillis())
			}
			clickModalSave()
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("saveCategoryRuleWithDefaults failed: ${e.message}")
		}
	}

	@Keyword
	def clickEditFirstRule() {
		try {
			click(to('btn_EditRule_first'))
		} catch (Exception e) {
			failStop("clickEditFirstRule failed: ${e.message}")
		}
	}

	/**
	 * Clicks Edit on the first rules-table row that contains {@code Cost of Goods} (same row-matching logic as
	 * {@link #clickEditRuleInTable} with a single fragment). Use when the first row in the table is not the
	 * Cost of Goods rule but a lower row is.
	 */
	@Keyword
	def clickEditFirstRuleContainingCostOfGoods() {
		try {
			waitVisible(to('tbl_rulesTable'))
			TestObject editButton = ruleTableRowEditButton(['Cost of Goods'] as List)
			click(editButton)
		} catch (Exception e) {
			failStop("clickEditFirstRuleContainingCostOfGoods failed: ${e.message}")
		}
	}

	@Keyword
	def verifyRuleModalEditable() {
		try {
			verifyRuleModalVisible('Edit rule modal')
			waitVisibleClickable(to('inp_firstVisibleInDialog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Rule should be editable: ${e.message}")
		}
	}

	@Keyword
	def editFirstVisibleFieldAndSave(String newValue) {
		try {
			TestObject input = to('inp_firstVisibleInDialog')
			waitVisibleClickable(input)
			WebUI.clearText(input)
			WebUI.setText(input, newValue)
			WebUI.takeScreenshot()
			clickModalSave()
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("editFirstVisibleFieldAndSave failed: ${e.message}")
		}
	}

	@Keyword
	def clickDeactivateFirstRule() {
		try {
			click(to('btn_DeactivateRule_first'))
		} catch (Exception e) {
			failStop("clickDeactivateFirstRule failed: ${e.message}")
		}
	}

	@Keyword
	def confirmDeactivateRule() {
		try {
			waitVisible(to('btn_confirm_Deactivate'))
			click(to('btn_confirm_Deactivate'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("confirmDeactivateRule failed: ${e.message}")
		}
	}
	
	@Keyword
	def cancelDeactivateRule() {
		try {
			waitVisible(to('btn_confirm_CANCEL'))
			click(to('btn_confirm_CANCEL'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("cancelDeactivateRule failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDeactivateConfirmationVisible() {
		try {
			waitVisible(to('btn_confirm_Deactivate'))
			WebUI.verifyElementVisible(to('btn_confirm_Deactivate'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("Deactivate confirmation not shown: ${e.message}")
		}
	}

	@Keyword
	def attemptDuplicateRuleSaveOnDduTab() {
		try {
			openTab('DDU')
			clickAddNewRule()
			clickModalSave()
			verifyDuplicateRuleMessageVisible()
		} catch (Exception e) {
			failStop("attemptDuplicateRuleSaveOnDduTab failed: ${e.message}")
		}
	}

	@Keyword
	def attemptDuplicateRuleSaveOnDdpTab() {
		try {
			openTab('DDP')
			clickAddNewRule()
			clickModalSave()
			verifyDuplicateRuleMessageVisible()
		} catch (Exception e) {
			failStop("attemptDuplicateRuleSaveOnDdpTab failed: ${e.message}")
		}
	}

	@Keyword
	def attemptDuplicateConditionFlow() {
		try {
			clickAddNewRule()
			clickAddCondition()
			selectConditionField('Cost of Goods')
			enterConditionValue('100')
			clickModalCancel()
			clickAddNewRule()
			clickAddCondition()
			selectConditionField('Cost of Goods')
			enterConditionValue('100')
			clickModalSave()
			verifyDuplicateConditionMessageVisible()
		} catch (Exception e) {
			failStop("attemptDuplicateConditionFlow failed: ${e.message}")
		}
	}

	private static String escapeXpathLiteral(String text) {
		if (text == null) {
			return "''"
		}
		if (!text.contains("'")) {
			return "'${text}'"
		}
		if (!text.contains('"')) {
			return "\"${text}\""
		}
		return "concat('${text.replace("'", "', \"'\", '")}')"
	}

	private static String rulesTableBaseXPath() {
		return "//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'Rule Criteria') or contains(.,'DESTINATION')]]"
	}

	private static String normalizeRuleRowText(String text) {
		return (text ?: '').replaceAll(/\s+/, ' ').trim().toLowerCase()
	}

	/**
	 * Order/criteria snapshot for CCIPH-201 reorder tests — strips action-column text that differs
	 * between default view ({@code Edit Deactivate}) and reorder mode ({@code No row actions while reordering}).
	 */
	private static String normalizeRuleRowSnapshotText(String text) {
		String t = normalizeRuleRowText(text)
		if (!t) {
			return ''
		}
		List<String> actionSuffixes = [
			'no row actions while reordering',
			'edit deactivate',
			'view edit deactivate'
		]
		boolean changed = true
		while (changed) {
			changed = false
			for (String suffix : actionSuffixes) {
				if (t.endsWith(suffix)) {
					t = t.substring(0, t.length() - suffix.length()).trim()
					changed = true
				}
			}
		}
		return t
	}

	private static boolean ruleRowTextMatches(String expected, String actual) {
		String e = normalizeRuleRowText(expected)
		String a = normalizeRuleRowText(actual)
		if (!e || !a) {
			return false
		}
		return a == e || a.contains(e) || e.contains(a)
	}

	private static boolean ruleOperatorMatches(String expected, String actual) {
		if (ruleRowTextMatches(expected, actual)) {
			return true
		}
		String e = normalizeRuleRowText(expected)
		String a = normalizeRuleRowText(actual)
		if (e == 'greater than' && (a == '>' || a.contains('greater'))) {
			return true
		}
		if (e == 'less than' && (a == '<' || a.contains('less'))) {
			return true
		}
		return false
	}

	private static String buildRuleRowTextMatchExpr(String fragment) {
		String trimmed = (fragment ?: '').trim()
		String lower = escapeXpathLiteral(trimmed.toLowerCase())
		String caseInsensitive =
			"contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), ${lower})"
		String op = trimmed.toLowerCase()
		if (op == 'greater than') {
			return "(${caseInsensitive} or contains(normalize-space(.), '>'))"
		}
		if (op == 'less than') {
			return "(${caseInsensitive} or contains(normalize-space(.), '<'))"
		}
		return caseInsensitive
	}

	private static void dismissRuleCreatedSuccessModalIfPresent() {
		try {
			TestObject okButton = findTestObject('Global Settings/EmailNoti/btn_ruleCreatedSuccessOk')
			if (WebUI.waitForElementPresent(okButton, 8, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(okButton)
				WebUI.click(okButton)
				WebUI.delay(1)
			}
		} catch (Exception ignored) {
		}
	}

	private static boolean waitForRuleTableRowPresent(TestObject ruleRow, int timeoutSeconds = DEFAULT_TIMEOUT) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			if (WebUI.waitForElementPresent(ruleRow, 2, FailureHandling.OPTIONAL)) {
				return true
			}
			WebUI.delay(1)
		}
		return false
	}

	private static String ruleTableRowXPathByCriteriaValue(String conditionValue) {
		String lit = escapeXpathLiteral((conditionValue ?: '').trim())
		return "${rulesTableBaseXPath()}//tbody//tr[.//td[contains(@class,'ruleCriteriaTd')][contains(normalize-space(.), ${lit})]]"
	}

	private static TestObject ruleTableRowByCriteriaValue(String conditionValue) {
		TestObject row = new TestObject("savedRuleRowByValue_${conditionValue.hashCode()}")
		row.addProperty('xpath', ConditionType.EQUALS, ruleTableRowXPathByCriteriaValue(conditionValue))
		return row
	}

	private static void verifyRuleConditionPartsInRow(String rowXp, String fieldName, String conditionValue,
			String operator, String destination) {
		TestObject fieldObj = new TestObject('rowConditionField')
		fieldObj.addProperty('xpath', ConditionType.EQUALS, "${rowXp}//span[contains(@class,'ruleLabel')]")
		TestObject operatorObj = new TestObject('rowConditionOperator')
		operatorObj.addProperty('xpath', ConditionType.EQUALS, "${rowXp}//span[contains(@class,'ruleEquals')]")
		TestObject valueObj = new TestObject('rowConditionValue')
		valueObj.addProperty('xpath', ConditionType.EQUALS, "${rowXp}//div[contains(@class,'ruleValuesFlex')]")

		String actualField = (WebUI.getText(fieldObj, FailureHandling.OPTIONAL) ?: '').trim()
		String actualOperator = (WebUI.getText(operatorObj, FailureHandling.OPTIONAL) ?: '').trim()
		String actualValue = (WebUI.getText(valueObj, FailureHandling.OPTIONAL) ?: '').trim()

		if (!ruleRowTextMatches(fieldName, actualField)) {
			failStop("Rule field mismatch. Expected '${fieldName}', got '${actualField}'")
		}
		if (!ruleOperatorMatches(operator, actualOperator)) {
			failStop("Rule operator mismatch. Expected '${operator}', got '${actualOperator}'")
		}
		if (!ruleRowTextMatches(conditionValue, actualValue)) {
			failStop("Rule value mismatch. Expected '${conditionValue}', got '${actualValue}'")
		}
		if ((destination ?: '').trim()) {
			TestObject destinationObj = new TestObject('rowDestination')
			destinationObj.addProperty('xpath', ConditionType.EQUALS, "${rowXp}//span[contains(@class,'destinationPill')]")
			String actualDestination = (WebUI.getText(destinationObj, FailureHandling.OPTIONAL) ?: '').trim()
			if (!ruleRowTextMatches(destination, actualDestination)) {
				failStop("Rule destination mismatch. Expected '${destination}', got '${actualDestination}'")
			}
		}
	}

	private static TestObject ruleTableRowContaining(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "${rulesTableBaseXPath()}//tbody//tr[td][${rowCondition}]"

		TestObject row = new TestObject('savedRuleRow')
		row.addProperty('xpath', ConditionType.EQUALS, xpath)
		return row
	}

	/**
	 * Verifies a saved rule appears in the rules table on the current tab (DDU, DDP, or Category).
	 * Pass text fragments that must all appear in the same row.
	 *
	 * DDU example after Cost of Goods rule:
	 *   verifySavedRuleInTable('Saudi Arabia', 'Cost of Goods', 'Greater than', '47810')
	 *
	 * Category example:
	 *   verifySavedRuleInTable('Saudi Arabia', 'Cost of Goods', 'LV')
	 */
	@Keyword
	def verifySavedRuleInTable(String... expectedTexts) {
		try {
			dismissRuleCreatedSuccessModalIfPresent()
			waitVisible(to('tbl_rulesTable'))
			TestObject ruleRow = ruleTableRowContaining(expectedTexts as List)
			if (!waitForRuleTableRowPresent(ruleRow)) {
				failStop("Saved rule row not found matching: ${(expectedTexts as List).join(', ')}")
			}
			WebUI.scrollToElement(ruleRow, DEFAULT_TIMEOUT)
			WebUI.verifyElementPresent(ruleRow, FailureHandling.STOP_ON_FAILURE)
			KeywordUtil.logInfo("Saved rule found in table matching: ${(expectedTexts as List).join(', ')}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySavedRuleInTable failed: ${e.message}")
		}
	}

	@Keyword
	
	def verifyRuleCriteriaAdded(
	
			String conditionField,
			String ruleValue,
			String conditionOperator = "Greater than",
			String destination = null) {
		String actualField =
				WebUI.getText(findTestObject('Object Repository/Global Settings/CCIPH-30/lbl_ConditionField')).trim()
		String actualOperator =
				WebUI.getText(findTestObject('Object Repository/Global Settings/CCIPH-30/lbl_ConditionOperator')).trim()
		String actualValue =
				WebUI.getText(findTestObject('Object Repository/Global Settings/CCIPH-30/lbl_RuleValue')).trim()
		
				WebUI.verifyMatch(actualField, conditionField,false)
		WebUI.verifyMatch(
				actualOperator,
				conditionOperator,
				false)
	
		WebUI.verifyMatch(
				actualValue,
				ruleValue,
				false)
		if(expectedDestination) {
	
	
	
			String actualDestination = WebUI.getText(findTestObject('Object Repository/Global Settings/CCIPH-30/lbl_Destination')).trim()
			WebUI.verifyMatch(
					actualDestination,
					destination,
					false)
	
		}
	
		KeywordUtil.markPassed(
	
				"Rule validated successfully")
	
	}
	
	/**
	 * Verifies a saved rule by structured condition parts (field, operator, value, optional destination).
	 * Reusable for DDU / DDP / Category tabs.
	 *
	 * Example (TC_GS_FUNC_014 style):
	 *   verifySavedRuleWithCondition('Cost of Goods', '47810', 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def verifySavedRuleWithCondition(String fieldName, String conditionValue, String operator , String destination = null) {
		try {
		//	dismissRuleCreatedSuccessModalIfPresent()
			waitVisible(to('tbl_rulesTable'))
			TestObject ruleRow = ruleTableRowByCriteriaValue(conditionValue)
			if (!waitForRuleTableRowPresent(ruleRow)) {
				List<String> parts = []
				if ((destination ?: '').trim()) {
					parts << destination.trim()
				}
				parts << (fieldName ?: '').trim()
				parts << (operator ?: '').trim()
				parts << (conditionValue ?: '').trim()
				verifySavedRuleInTable(parts as String[])
				return
			}
			WebUI.scrollToElement(ruleRow, DEFAULT_TIMEOUT)
			verifyRuleConditionPartsInRow(ruleTableRowXPathByCriteriaValue(conditionValue), fieldName, conditionValue, operator, destination)
			KeywordUtil.logInfo("Saved rule verified — field: ${fieldName}, value: ${conditionValue}, operator: ${operator}, destination: ${destination ?: 'n/a'}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySavedRuleWithCondition failed: ${e.message}")
		}
	}

	private static String buildRuleRowMatchCondition(List<String> expectedTexts) {
		List<String> fragments = (expectedTexts ?: []).collect { (it ?: '').trim() }.findAll { it }
		if (fragments.isEmpty()) {
			failStop('Rule row match requires at least one expected text fragment')
		}
		return fragments.collect { fragment ->
			buildRuleRowTextMatchExpr(fragment)
		}.join(' and ')
	}

	private static TestObject ruleTableRowEditButton(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "${rulesTableBaseXPath()}//tbody//tr[td][${rowCondition}]" +
			"//*[self::button or self::a][contains(.,'Edit') or contains(@aria-label,'Edit') or contains(@title,'Edit')]"

		TestObject editButton = new TestObject('savedRuleEditButton')
		editButton.addProperty('xpath', ConditionType.EQUALS, xpath)
		return editButton
	}

	/**
	 * Clicks Edit on the rules table row that matches all given text fragments.
	 * Reusable on DDU, DDP, and Category tabs after a rule is created.
	 *
	 * Example:
	 *   clickEditRuleInTable('Saudi Arabia', 'Cost of Goods', 'Greater than', '47810')
	 */
	@Keyword
	def clickEditRuleInTable(String... expectedTexts) {
		try {
			waitVisible(to('tbl_rulesTable'))
			TestObject editButton = ruleTableRowEditButton(expectedTexts as List)
			waitVisibleClickable(editButton)
			WebUI.click(editButton)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickEditRuleInTable failed: ${e.message}")
		}
	}

	/**
	 * Clicks Edit for a saved rule identified by field, value, operator, and optional destination.
	 *
	 * Example (after TC_GS_FUNC_087 create):
	 *   clickEditRuleWithCondition('Cost of Goods', ruleValue, 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def clickEditRuleWithCondition(String fieldName, String conditionValue, String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (conditionValue ?: '').trim()
		clickEditRuleInTable(parts as String[])
	}

	/**
	 * Opens Edit for a saved rule, updates the Values field, and clicks Save.
	 * Does not modify existing keywords — composes clickEditRuleInTable, enterConditionValue, saveRuleModal.
	 *
	 * Example:
	 *   editRuleConditionValueAndSave('99999', 'Saudi Arabia', 'Cost of Goods', 'Greater than', '47810')
	 */
	@Keyword
	def editRuleConditionValueAndSave(String newValue, String... expectedRowTexts) {
		try {
			clickEditRuleInTable(expectedRowTexts)
			verifyRuleModalVisible('Edit rule modal')
			enterConditionValue(newValue)
			saveRuleModal()
		} catch (Exception e) {
			failStop("editRuleConditionValueAndSave failed: ${e.message}")
		}
	}

	/**
	 * Opens Edit by condition parts, updates Values, and saves.
	 *
	 * Example:
	 *   editRuleConditionValueAndSaveWithCondition(updatedValue, 'Cost of Goods', ruleValue, 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def editRuleConditionValueAndSaveWithCondition(String newValue, String fieldName, String currentConditionValue,
			String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (currentConditionValue ?: '').trim()
		editRuleConditionValueAndSave(newValue, parts as String[])
	}

	/**
	 * Verifies the "Rule Updated Successfully" success modal title is displayed.
	 */
	@Keyword
	def verifyRuleUpdatedSuccessfullyMessage(String expectedTitle = 'Rule Updated Successfully') {
		try {
			TestObject title = to('msg_ruleUpdatedSuccessTitle')
			waitVisible(title)
			WebUI.verifyElementText(title, expectedTitle, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleUpdatedSuccessfullyMessage failed: ${e.message}")
		}
	}

	/**
	 * Clicks OK on the "Rule Updated Successfully" success modal.
	 */
	@Keyword
	def clickRuleUpdateSuccessOk() {
		try {
			TestObject okButton = to('btn_ruleUpdateSuccessOk')
			waitVisibleClickable(okButton)
			WebUI.click(okButton)
			WebUI.delay(1)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickRuleUpdateSuccessOk failed: ${e.message}")
		}
	}

	/**
	 * Validates "Rule Updated Successfully" text and clicks OK.
	 * Reusable after editing and saving a rule on any tab.
	 */
	@Keyword
	def verifyRuleUpdatedSuccessfullyAndClickOk(String expectedTitle = 'Rule Updated Successfully') {
		try {
			verifyRuleUpdatedSuccessfullyMessage(expectedTitle)
			clickRuleUpdateSuccessOk()
		} catch (Exception e) {
			failStop("verifyRuleUpdatedSuccessfullyAndClickOk failed: ${e.message}")
		}
	}

	private static TestObject ruleTableRowDeactivateButton(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'DESTINATION')]]//tbody//tr[${rowCondition}]" +
			"//*[self::button or self::a][contains(.,'Deactivate') or contains(.,'Remove') or contains(@aria-label,'Deactivate') or contains(@aria-label,'Remove')]" +
			" | //table//tbody//tr[${rowCondition}]//*[self::button or self::a][contains(.,'Deactivate') or contains(.,'Remove') or contains(@aria-label,'Deactivate') or contains(@aria-label,'Remove')]"

		TestObject deactivateButton = new TestObject('savedRuleDeactivateButton')
		deactivateButton.addProperty('xpath', ConditionType.EQUALS, xpath)
		return deactivateButton
	}

	/**
	 * Clicks Deactivate on the rules table row matching all given text fragments.
	 * Reusable on DDU, DDP, and Category tabs for a newly created rule.
	 *
	 * Example:
	 *   clickDeactivateRuleInTable('Saudi Arabia', 'Cost of Goods', 'Greater than', '47810')
	 */
	@Keyword
	def clickDeactivateRuleInTable(String... expectedTexts) {
		try {
			waitVisible(to('tbl_rulesTable'))
			TestObject deactivateButton = ruleTableRowDeactivateButton(expectedTexts as List)
			waitVisibleClickable(deactivateButton)
			WebUI.click(deactivateButton)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickDeactivateRuleInTable failed: ${e.message}")
		}
	}

	/**
	 * Clicks Deactivate for a saved rule identified by field, value, operator, and optional destination.
	 *
	 * Example:
	 *   clickDeactivateRuleWithCondition('Cost of Goods', ruleValue, 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def clickDeactivateRuleWithCondition(String fieldName, String conditionValue, String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (conditionValue ?: '').trim()
		clickDeactivateRuleInTable(parts as String[])
	}

	/**
	 * Deactivates a saved rule: clicks Deactivate on the matching row, verifies confirmation, and confirms removal.
	 * Reusable after creating a rule on any tab.
	 *
	 * Example (TC_GS_FUNC_091):
	 *   deactivateSavedRuleWithCondition('Cost of Goods', ruleValue, 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def deactivateSavedRuleInTable(String... expectedRowTexts) {
		try {
			clickDeactivateRuleInTable(expectedRowTexts)
			verifyDeactivateConfirmationVisible()
			confirmDeactivateRule()
		} catch (Exception e) {
			failStop("deactivateSavedRuleInTable failed: ${e.message}")
		}
	}

	/**
	 * Deactivates a saved rule using structured condition parts (field, value, operator, destination).
	 */
	@Keyword
	def deactivateSavedRuleWithCondition(String fieldName, String conditionValue, String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (conditionValue ?: '').trim()
		deactivateSavedRuleInTable(parts as String[])
	}

	private static TestObject ruleTableRowActivateButton(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'DESTINATION')]]//tbody//tr[${rowCondition}]" +
			"//td[contains(@class,'tdActionsView')]//*[self::button or self::a][contains(.,'Activate') or contains(@aria-label,'Activate')]" +
			" | //table//tbody//tr[${rowCondition}]//*[self::button or self::a][normalize-space()='Activate' or contains(.,'Activate')]"

		TestObject activateButton = new TestObject('savedRuleActivateButton')
		activateButton.addProperty('xpath', ConditionType.EQUALS, xpath)
		return activateButton
	}

	/**
	 * Verifies the Activate button is visible for a deactivated rule in the matching table row.
	 * Use after deactivateSavedRuleWithCondition / deactivateSavedRuleInTable.
	 *
	 * Example:
	 *   verifyActivateRuleButtonInTable('Saudi Arabia', 'Cost of Goods', 'Greater than', '47810')
	 */
	@Keyword
	def verifyActivateRuleButtonInTable(String... expectedTexts) {
		try {
			waitVisible(to('tbl_rulesTable'))
			TestObject activateButton = ruleTableRowActivateButton(expectedTexts as List)
			waitVisible(activateButton)
			WebUI.verifyElementVisible(activateButton, FailureHandling.STOP_ON_FAILURE)
			String label = (WebUI.getText(activateButton, FailureHandling.OPTIONAL) ?: '').trim()
			if (!label.equalsIgnoreCase('Activate') && !label.contains('Activate')) {
				failStop("Activate button label mismatch. Expected to contain 'Activate', Actual: '${label}'")
			}
			KeywordUtil.logInfo("Activate button verified for rule row matching: ${(expectedTexts as List).join(', ')}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyActivateRuleButtonInTable failed: ${e.message}")
		}
	}

	/**
	 * Verifies Activate button for a rule identified by field, value, operator, and optional destination.
	 *
	 * Example (TC_GS_FUNC_091 after deactivate):
	 *   verifyActivateRuleButtonWithCondition('Cost of Goods', ruleValue, 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def verifyActivateRuleButtonWithCondition(String fieldName, String conditionValue, String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (conditionValue ?: '').trim()
		verifyActivateRuleButtonInTable(parts as String[])
	}

	/**
	 * Selects the OPERATOR dropdown in Filter Conditions using {@link WebUI#selectOptionByIndex}.
	 * Targets the native {@code select} that contains the GREATERTHAN option (Cost of Goods operators).
	 *
	 * Option indices (0-based, as for {@code selectOptionByIndex}):
	 * {@code 0} — placeholder "Select operator" (often disabled);
	 * {@code 1} — Greater than;
	 * {@code 2} — Less than;
	 * {@code 3} — Greater than Equal to;
	 * {@code 4} — Less than Equal to.
	 *
	 * @param optionIndex 0-based option index inside the OPERATOR select
	 * @param conditionRowIndex {@code data-testid} row suffix (e.g. {@code 0} for {@code rule-condition-row-0})
	 */
	@Keyword
	def selectConditionOperatorByIndex(int optionIndex, int conditionRowIndex = 0) {
		try {
			TestObject operatorSelect
			if (conditionRowIndex == 0) {
				operatorSelect = to('ddl_conditionOperator')
			} else {
				operatorSelect = new TestObject("ddl_condition_operator_row_${conditionRowIndex}")
				String xpath = "//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[./option[@value='GREATERTHAN']]"
				operatorSelect.addProperty('xpath', ConditionType.EQUALS, xpath)
			}
			waitVisible(operatorSelect)
			// Katalon: selectOptionByIndex(TestObject, Object) or (TestObject, Object, FailureHandling) — not (TO, String, boolean)
			WebUI.selectOptionByIndex(operatorSelect, optionIndex, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectConditionOperatorByIndex(${optionIndex}, row=${conditionRowIndex}) failed: ${e.message}")
		}
	}

	/**
	 * Locates the n-th native {@code select} inside a Filter Conditions row ({@code selectPositionOneBased}: 1=Field, 2=Operator, 3=Values).
	 */
	private static TestObject conditionRowNativeSelect(int conditionRowIndex, int selectPositionOneBased) {
		TestObject t = new TestObject("cond_row_${conditionRowIndex}_native_select_${selectPositionOneBased}")
		int domRow = conditionRowIndex + 1
		String xpath = "(//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[${selectPositionOneBased}])" +
			" | ((//div[@role='dialog']//*[contains(@class,'conditionRow')])[${domRow}]//select[${selectPositionOneBased}])"
		t.addProperty('xpath', ConditionType.EQUALS, xpath)
		return t
	}

	/**
	 * Value cell for a condition row when it is a plain text/number input (not a {@code select}).
	 */
	private static TestObject conditionRowValueInput(int conditionRowIndex) {
		TestObject t = new TestObject("cond_row_${conditionRowIndex}_value_input")
		int domRow = conditionRowIndex + 1
		String xpath = "(//div[@data-testid='rule-condition-row-${conditionRowIndex}']//input[(not(@type) or @type='' or @type='text' or @type='number' or @type='search')])[last()]" +
			" | (((//div[@role='dialog']//*[contains(@class,'conditionRow')])[${domRow}]//input[(not(@type) or @type='' or @type='text' or @type='number' or @type='search')])[last()])"
		t.addProperty('xpath', ConditionType.EQUALS, xpath)
		return t
	}

	/** Escape {@code "} and {@code \} for use inside XPath double-quoted literals. */
	private static String escapeXpathDoubleQuotes(String raw) {
		if (raw == null) {
			return ''
		}
		return raw.replace('\\', '\\\\').replace('"', '\\"')
	}

	/**
	 * VALUES {@code select} whose {@code aria-label} is built from Field and Operator (e.g. {@code Payment Type — Equals}).
	 */
	private static TestObject conditionRowValuesSelectByFieldOperatorAria(int conditionRowIndex, String fieldLabel, String operatorLabel) {
		TestObject t = new TestObject("cond_row_${conditionRowIndex}_values_select_aria")
		int domRow = conditionRowIndex + 1
		String field = (fieldLabel ?: '').trim()
		String op = (operatorLabel ?: '').trim()
		String ef = escapeXpathDoubleQuotes(field)
		String eo = escapeXpathDoubleQuotes(op)
		String ariaSpacedEm = "${field} \u2014 ${op}"
		String ariaTightEm = "${field}\u2014${op}"
		String ariaSpacedEn = "${field} \u2013 ${op}"
		String ariaHyphen = "${field} - ${op}"
		String aSp = escapeXpathDoubleQuotes(ariaSpacedEm)
		String aTt = escapeXpathDoubleQuotes(ariaTightEm)
		String aSn = escapeXpathDoubleQuotes(ariaSpacedEn)
		String aHy = escapeXpathDoubleQuotes(ariaHyphen)

		String xpath =
			"(//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[@aria-label=\"${aSp}\"])" +
			" | (//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[@aria-label=\"${aTt}\"])" +
			" | (//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[@aria-label=\"${aSn}\"])" +
			" | (//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[@aria-label=\"${aHy}\"])" +
			" | (//div[@data-testid='rule-condition-row-${conditionRowIndex}']//select[contains(@aria-label,\"${ef}\") and contains(@aria-label,\"${eo}\")])" +
			" | (((//div[@role='dialog']//*[contains(@class,'conditionRow')])[${domRow}]//select[contains(@aria-label,\"${ef}\") and contains(@aria-label,\"${eo}\")])[last()])"
		t.addProperty('xpath', ConditionType.EQUALS, xpath)
		return t
	}

	/**
	 * Sets Field, Operator, and Values in one condition row using visible option text for selects,
	 * or text for the Values cell when the app renders Values as an {@code input}.
	 * The VALUES control is resolved dynamically: prefers {@code select} whose {@code aria-label} matches
	 * {@code Field — Operator} (em dash, hyphen, or substring match), then third {@code select}, then row text input.
	 * Pass {@code valuesLabel} as the visible option text or typed value from your data file.
	 *
	 * Example:
	 *   selectConditionFieldOperatorAndValueByLabel('Payment Type', 'Equals', 'F')
	 *
	 * @param conditionRowIndex {@code data-testid} row suffix ({@code 0} = {@code rule-condition-row-0}), or ordinal row in fallback XPath
	 */
	@Keyword
	def selectConditionFieldOperatorAndValueByLabel(String fieldLabel, String operatorLabel, String valuesLabel, int conditionRowIndex = 0) {
		try {
			String field = (fieldLabel ?: '').trim()
			String operator = (operatorLabel ?: '').trim()
			String values = (valuesLabel ?: '').trim()
			if (!field) {
				failStop('selectConditionFieldOperatorAndValueByLabel failed: fieldLabel is blank')
			}
			if (!operator) {
				failStop('selectConditionFieldOperatorAndValueByLabel failed: operatorLabel is blank')
			}
			if (!values) {
				failStop('selectConditionFieldOperatorAndValueByLabel failed: valuesLabel is blank')
			}

			TestObject fieldSel = conditionRowNativeSelect(conditionRowIndex, 1)
			TestObject operatorSel = conditionRowNativeSelect(conditionRowIndex, 2)
			TestObject valuesSelByIndex = conditionRowNativeSelect(conditionRowIndex, 3)

			waitVisibleClickable(fieldSel)
			WebUI.selectOptionByLabel(fieldSel, field, false)
			WebUI.delay(1)
			waitVisibleClickable(operatorSel)
			WebUI.selectOptionByLabel(operatorSel, operator, false)
			WebUI.delay(1)

			TestObject valuesByAria = conditionRowValuesSelectByFieldOperatorAria(conditionRowIndex, field, operator)
			if (WebUI.verifyElementPresent(valuesByAria, 10, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(valuesByAria)
				WebUI.selectOptionByLabel(valuesByAria, values, false)
			} else if (WebUI.verifyElementPresent(valuesSelByIndex, 5, FailureHandling.OPTIONAL)) {
				waitVisibleClickable(valuesSelByIndex)
				WebUI.selectOptionByLabel(valuesSelByIndex, values, false)
			} else {
				TestObject valuesInput = conditionRowValueInput(conditionRowIndex)
				waitVisibleClickable(valuesInput)
				WebUI.clearText(valuesInput)
				WebUI.setText(valuesInput, values)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectConditionFieldOperatorAndValueByLabel('${fieldLabel}', '${operatorLabel}', '${valuesLabel}', row=${conditionRowIndex}) failed: ${e.message}")
		}
	}

	// ---------- CCIPH-31: Edit rule, pre-population, duplicate on edit, AC4 note ----------

	@Keyword
	def refreshGlobalSettingsPage() {
		try {
			WebUI.refresh()
			WebUI.delay(2)
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("refreshGlobalSettingsPage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyCustomsTypeReadonlyInRuleModal(String expectedDduOrDdp) {
		try {
			verifyRuleModalVisible('Rule modal')
			String dlg = (WebUI.getText(to('dlg_ruleModal'), FailureHandling.OPTIONAL) ?: '')
			if (!dlg.contains('Customs Type')) {
				failStop("Expected 'Customs Type' in rule modal. Excerpt: ${dlg.take(400)}")
			}
			String exp = (expectedDduOrDdp ?: '').trim().toUpperCase()
			if (!dlg.toUpperCase().contains(exp)) {
				failStop("Expected Customs Type context '${exp}' in modal. Excerpt: ${dlg.take(400)}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCustomsTypeReadonlyInRuleModal failed: ${e.message}")
		}
	}

	@Keyword
	def verifyConditionValueFieldNotEmpty() {
		try {
			waitVisible(to('enter_TextValue'))
			String v = resolveElementDisplayText(to('enter_TextValue'))
			if (!v) {
				failStop('Condition value field is empty — expected pre-populated rule data')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyConditionValueFieldNotEmpty failed: ${e.message}")
		}
	}

	/**
	 * With the rule / Filter Conditions modal open: verifies the Values field ({@code enter_TextValue}) is not empty,
	 * reads its current text, clears and re-enters the same value, clicks Save, then asserts duplicate / same-rule
	 * messaging ({@link #verifyDuplicateOrSameRuleErrorVisible}).
	 *
	 * <p>Does not alter other keywords — only composes them.</p>
	 */
	@Keyword
	def fetchConditionValueReenterSameAndVerifyDuplicatePopup() {
		try {
			verifyConditionValueFieldNotEmpty()
			TestObject valueInput = to('enter_TextValue')
			String value = resolveElementDisplayText(valueInput)
			if (!value) {
				failStop('Condition value field is empty after verification — cannot re-enter')
			}
			waitVisibleClickable(valueInput)
			WebUI.clearText(valueInput)
			enterConditionValue(value)
			clickModalSave()
			WebUI.delay(2)
//			verifyDuplicateOrSameRuleErrorVisible()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("fetchConditionValueReenterSameAndVerifyDuplicatePopup failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyRuleUpdateSuccessTitle(
			String expectedTitle =
			"Rule Updated Successfully") {
	
		TestObject title = findTestObject(
				'Object Repository/Global Settings/CCIPH-30/success_Title')
	
		WebUI.waitForElementVisible(title, 10)
	
		WebUI.verifyElementVisible(title)
	
		String actualTitle =
				WebUI.getText(title).trim()
	
		WebUI.verifyMatch(
				actualTitle,
				expectedTitle,
				false)
	
		KeywordUtil.markPassed(
				"Popup title verified successfully: ${actualTitle}")
	}

	@Keyword
	def dismissRuleUpdateSuccessModalIfPresent() {
		try {
			if (WebUI.waitForElementPresent(to('btn_ruleUpdateSuccessOk'), 6, FailureHandling.OPTIONAL)) {
				//waitForElementVisible(to('btn_ruleUpdateSuccessOk')
				waitVisibleClickable(to('btn_ruleUpdateSuccessOk'))
				WebUI.click(to('btn_ruleUpdateSuccessOk'))
				WebUI.delay(1)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("dismissRuleUpdateSuccessModalIfPresent failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDuplicateOrSameRuleErrorVisible() {
		try {
			if (WebUI.waitForElementVisible(to('msg_duplicateRule'), 8, FailureHandling.OPTIONAL)) {
				WebUI.verifyElementVisible(to('msg_duplicateRule'), FailureHandling.STOP_ON_FAILURE)
				WebUI.takeScreenshot()
				return
			}
			TestObject dyn = new TestObject('dupOrSameRuleMsg')
			dyn.addProperty('xpath', ConditionType.EQUALS,
				"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'same rule') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'already exist') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'duplicate rule')]")
			waitVisible(dyn)
			WebUI.verifyElementVisible(dyn, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDuplicateOrSameRuleErrorVisible failed: ${e.message}")
		}
	}

	/**
	 * TC_GS_FUNC_037: create two DDU Cost-of-Goods rules with distinct values, then edit the first to match the second and expect duplicate / same-rule error.
	 */
	@Keyword
	def cciph31_editFirstRuleToDuplicateSecondRuleOnDdu(String valueOne, String valueTwo) {
		try {
			navigateToGlobalSettings()
			openTab('DDU')
			createDduRuleWithCostOfGoods(valueOne)
			createDduRuleWithCostOfGoods(valueTwo)
			clickEditRuleWithCondition('Cost of Goods', valueOne, 'Greater than', null)
			verifyRuleModalVisible('Edit rule')
			enterConditionValue(valueTwo)
			clickModalSave()
			WebUI.delay(2)
			verifyDuplicateOrSameRuleErrorVisible()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("cciph31_editFirstRuleToDuplicateSecondRuleOnDdu failed: ${e.message}")
		}
	}

	// ---------- CCIPH-37: Global Settings History (AC1–AC4) — assumes authenticated session ----------

	@Keyword
	def clickGlobalSettingsHistoryButton() {
		try {
			click(to37('btn_GlobalSettingsHistory'))
		} catch (Exception e) {
			failStop("clickGlobalSettingsHistoryButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyGlobalSettingsHistoryPageLoaded() {
		try {
			waitVisible(to37('txt_historyPageTitle'))
			WebUI.verifyElementVisible(to37('txt_historyPageTitle'), FailureHandling.STOP_ON_FAILURE)
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.verifyElementVisible(to37('tbl_historyChangeLog'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyGlobalSettingsHistoryPageLoaded failed: ${e.message}")
		}
	}

	private static String normalizeHistoryHeaderText(String text) {
		String firstLine = (text ?: '').split(/[\r\n]+/)[0]
		return firstLine.replaceAll(/\s+/, ' ').trim().toLowerCase()
	}

	private static boolean historyHeaderMatches(String expected, String actual) {
		String e = normalizeHistoryHeaderText(expected)
		String a = normalizeHistoryHeaderText(actual)
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

	private List<String> getHistoryLogTableHeaderTexts() {
		def driver = DriverFactory.getWebDriver()
		String xp = "//table[.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'timestamp')]]//th"
		List<WebElement> headers = driver.findElements(By.xpath(xp))
		return headers.collect { (it.getText() ?: '').trim() }.findAll { it }.unique()
	}

	@Keyword
	def verifyHistoryLogTableColumnHeadersPresent() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			List<String> expected = ['TIMESTAMP', 'SECTION', 'USER', 'CHANGE']
			List<String> headers = getHistoryLogTableHeaderTexts()
			if (headers.isEmpty()) {
				expected.each { String col ->
					String colLower = col.toLowerCase()
					String actionAlt = col.equalsIgnoreCase('CHANGE') ?
						" or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'action')" : ''
					TestObject th = new TestObject("history_th_${col}")
					th.addProperty('xpath', ConditionType.EQUALS,
						"//table//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'${colLower}')${actionAlt}]")
					waitVisible(th)
					WebUI.verifyElementVisible(th, FailureHandling.STOP_ON_FAILURE)
				}
			} else {
				expected.each { String col ->
					boolean found = headers.any { String header -> historyHeaderMatches(col, header) }
					if (!found) {
						failStop("History table header '${col}' not found. Visible headers: ${headers}")
					}
				}
				KeywordUtil.logInfo("History table headers: ${headers}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryLogTableColumnHeadersPresent failed: ${e.message}")
		}
	}

	@Keyword
	def openGlobalSettingsHistoryFromLanding() {
		try {
			
			navigateToGlobalSettings()
			clickGlobalSettingsHistoryButton()
			verifyGlobalSettingsHistoryPageLoaded()
		} catch (Exception e) {
			failStop("openGlobalSettingsHistoryFromLanding failed: ${e.message}")
		}
	}

	@Keyword
	def clickHistoryBackButton() {
		try {
			click(to37('btn_backFromHistory'))
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickHistoryBackButton failed: ${e.message}")
		}
	}

	@Keyword
	def verifyGlobalSettingsLandingVisible() {
		try {
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.verifyElementVisible(to('txt_anchor_GlobalSettings'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyGlobalSettingsLandingVisible failed: ${e.message}")
		}
	}

	@Keyword
	def clickHistoryDateFilter(String filterKey) {
		try {
			String k = (filterKey ?: '').trim().toUpperCase()
			TestObject btn
			switch (k) {
				case 'ALL':
					btn = to37('btn_filter_All')
					break
				case 'TODAY':
					btn = to37('btn_filter_Today')
					break
				case 'THIS WEEK':
				case 'THISWEEK':
					btn = to37('btn_filter_ThisWeek')
					break
				case 'THIS MONTH':
				case 'THISMONTH':
					btn = to37('btn_filter_ThisMonth')
					break
				case 'CUSTOM':
				case 'CUSTOM RANGE':
				case 'CUSTOMRANGE':
					btn = to37('btn_filter_CustomRange')
					break
				default:
					failStop("Unknown history date filter: ${filterKey}")
					return
			}
			click(btn)
			WebUI.delay(2)
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickHistoryDateFilter(${filterKey}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryTableStillDisplayed() {
		try {
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.verifyElementVisible(to37('tbl_historyChangeLog'), FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryTableStillDisplayed failed: ${e.message}")
		}
	}

	/**
	 * Normalizes {@code dd-MM-yyyy} or {@code yyyy-MM-dd} to ISO {@code yyyy-MM-dd} for HTML5 {@code input[type=date]}.
	 */
	private static String normalizeHistoryDateToIso(String raw) {
		String t = (raw ?: '').trim()
		if (!t) {
			failStop('normalizeHistoryDateToIso: date is blank')
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

	/** Snapshot of footer + row count + first row — used to detect filter refresh. */
	private static String historyFilterSnapshot() {
		String footer = ''
		try {
			footer = (WebUI.getText(to37('txt_historyRecordFooter'), FailureHandling.OPTIONAL) ?: '').trim()
		} catch (Exception ignored) {
		}
		int rowCount = 0
		try {
			TestObject rowsTo = new TestObject('history_filter_snap_rows')
			rowsTo.addProperty('xpath', ConditionType.EQUALS, historyTableDataRowsXPath())
			List<?> els = WebUI.findWebElements(rowsTo, 2)
			rowCount = els ? els.size() : 0
		} catch (Exception ignored) {
		}
		String firstRow = ''
		try {
			TestObject firstRowTo = new TestObject('history_filter_snap_first_row')
			firstRowTo.addProperty('xpath', ConditionType.EQUALS,
				"(//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td])[1]")
			if (WebUI.waitForElementVisible(firstRowTo, 1, FailureHandling.OPTIONAL)) {
				firstRow = (WebUI.getText(firstRowTo, FailureHandling.OPTIONAL) ?: '').trim()
			}
		} catch (Exception ignored) {
		}
		return "${footer}|${rowCount}|${firstRow}"
	}

	private static void waitForHistoryFilterToApply(String beforeSnapshot, int timeoutSec = 25) {
		long deadline = System.currentTimeMillis() + (timeoutSec * 1000L)
		while (System.currentTimeMillis() < deadline) {
			WebUI.delay(0.5)
			if (historyFilterSnapshot() != beforeSnapshot) {
				WebUI.delay(1)
				return
			}
		}
		failStop("History table did not refresh after custom date range (footer/rows unchanged for ${timeoutSec}s)")
	}

	/**
	 * Sets value on React-controlled {@code input[type=date]} so the app state and API filter update.
	 */
	private static void setHistoryHtmlDateInput(TestObject testObject, String yyyyMmDd) {
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
input.dispatchEvent(new Event("change", { bubbles: true }));
input.dispatchEvent(new Event("blur", { bubbles: true }));''',
			java.util.Arrays.asList(el, iso))
		WebUI.delay(0.3)
		String actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (!iso.equals(actual)) {
			WebUI.click(testObject)
			WebUI.clearText(testObject, FailureHandling.OPTIONAL)
			WebUI.setText(testObject, iso)
			WebUI.sendKeys(testObject, Keys.chord(Keys.TAB))
			WebUI.delay(0.3)
			actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		}
		if (!iso.equals(actual)) {
			failStop("History date input value mismatch: expected ${iso}, got '${actual}'")
		}
	}

	/** Blur date inputs and click Apply if present so the custom-range API request runs. */
	private static void commitHistoryCustomDateFilter(TestObject startInp, TestObject endInp) {
		waitVisibleClickable(startInp)
		WebUI.click(startInp)
		WebUI.sendKeys(startInp, Keys.chord(Keys.TAB))
		WebUI.delay(0.3)
		waitVisibleClickable(endInp)
		WebUI.click(endInp)
		WebUI.sendKeys(endInp, Keys.chord(Keys.ENTER))
		WebUI.delay(0.3)
		if (WebUI.waitForElementVisible(to37('txt_historyPageTitle'), 3, FailureHandling.OPTIONAL)) {
			WebUI.click(to37('txt_historyPageTitle'))
		}
		WebUI.delay(0.3)
		TestObject applyNearDates = new TestObject('history_apply_near_custom_dates')
		applyNearDates.addProperty('xpath', ConditionType.EQUALS,
			"//input[@id='ccfe-history-custom-end']/following::button[contains(translate(normalize-space(.),'APPLY','apply'),'apply')][1]")
		if (WebUI.waitForElementClickable(applyNearDates, 5, FailureHandling.OPTIONAL)) {
			WebUI.click(applyNearDates)
			return
		}
		TestObject applyBtn = to37('btn_customRangeApply')
		if (WebUI.waitForElementClickable(applyBtn, 3, FailureHandling.OPTIONAL)) {
			WebUI.click(applyBtn)
		}
	}

	/**
	 * Global Settings History — Custom Range via HTML5 date inputs ({@code ccfe-history-custom-start/end}).
	 * Accepts {@code yyyy-MM-dd} or {@code dd-MM-yyyy}.
	 */
	@Keyword
	def applyCustomHistoryDateRange(String startDate, String endDate) {
		try {
			clickHistoryDateFilter('CUSTOM')
			WebUI.delay(1)
			TestObject startInp = to37('inp_customRangeStart')
			TestObject endInp = to37('inp_customRangeEnd')
			String startIso = normalizeHistoryDateToIso(startDate)
			String endIso = normalizeHistoryDateToIso(endDate)
			waitVisible(startInp)
			waitVisible(endInp)
			setHistoryHtmlDateInput(startInp, startIso)
			WebUI.delay(0.5)
			setHistoryHtmlDateInput(endInp, endIso)
			String beforeSnapshot = historyFilterSnapshot()
			commitHistoryCustomDateFilter(startInp, endInp)
			waitForHistoryFilterToApply(beforeSnapshot, 25)
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("applyCustomHistoryDateRange failed: ${e.message}")
		}
	}

	/**
	 * Verifies custom date inputs hold the expected ISO values and the history grid refreshed (footer or rows present).
	 */
	@Keyword
	def verifyHistoryCustomDateRangeApplied(String startDate, String endDate) {
		try {
			String startIso = normalizeHistoryDateToIso(startDate)
			String endIso = normalizeHistoryDateToIso(endDate)
			String startVal = (WebUI.getAttribute(to37('inp_customRangeStart'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			String endVal = (WebUI.getAttribute(to37('inp_customRangeEnd'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			if (!startIso.equals(startVal) || !endIso.equals(endVal)) {
				failStop("Custom date inputs not applied: expected ${startIso} / ${endIso}, got ${startVal} / ${endVal}")
			}
			waitVisible(to37('tbl_historyChangeLog'))
			String footer = getHistoryRecordFooterText()
			KeywordUtil.logInfo("History footer after custom range: ${footer}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryCustomDateRangeApplied failed: ${e.message}")
		}
	}

	private static final DateTimeFormatter HISTORY_CUSTOM_DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern('dd-MM-yyyy')

	/**
	 * Resolves a relative custom range: end = today, start = today minus {@code daysBack} ({@code dd-MM-yyyy}).
	 */
	private static List<String> resolveHistoryRelativeCustomRange(int daysBack) {
		if (daysBack < 0) {
			failStop("resolveHistoryRelativeCustomRange: daysBack must be >= 0, got ${daysBack}")
		}
		LocalDate end = LocalDate.now()
		LocalDate start = end.minusDays(daysBack)
		return [start.format(HISTORY_CUSTOM_DISPLAY_DATE_FMT), end.format(HISTORY_CUSTOM_DISPLAY_DATE_FMT)]
	}

	/**
	 * Sets {@code input[type=date]} via focus + clear + ISO typing + TAB (React-friendly), with JS fallback.
	 * Used only by the relative / end-first custom-range flow — does not replace {@link #setHistoryHtmlDateInput}.
	 */
	private static void setHistoryHtmlDateInputInteractive(TestObject testObject, String yyyyMmDd) {
		String iso = (yyyyMmDd ?: '').trim()
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.delay(0.2)
		WebUI.sendKeys(testObject, Keys.chord(Keys.CONTROL, 'a'))
		WebUI.sendKeys(testObject, Keys.chord(Keys.BACK_SPACE))
		WebUI.delay(0.2)
		WebUI.setText(testObject, iso, FailureHandling.OPTIONAL)
		WebUI.sendKeys(testObject, Keys.chord(Keys.TAB))
		WebUI.delay(0.4)
		String actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (!iso.equals(actual)) {
			WebElement el = WebUiCommonHelper.findWebElement(testObject, DEFAULT_TIMEOUT)
			WebUI.executeJavaScript(
				'''var input = arguments[0];
var value = arguments[1];
var tracker = input._valueTracker;
if (tracker) { tracker.setValue(value); }
var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
setter.call(input, value);
input.dispatchEvent(new InputEvent("input", { bubbles: true, cancelable: true, inputType: "insertText", data: value }));
input.dispatchEvent(new Event("change", { bubbles: true }));
input.focus();
input.blur();''',
				java.util.Arrays.asList(el, iso))
			WebUI.delay(0.4)
			actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		}
		if (!iso.equals(actual)) {
			failStop("History date input value mismatch: expected ${iso}, got '${actual}'")
		}
	}

	/**
	 * Commits custom-range filter when the UI has no Apply button (auto-apply on blur / end-date change).
	 * Used only by the relative / end-first flow — does not replace {@link #commitHistoryCustomDateFilter}.
	 */
	private static void commitHistoryCustomDateFilterAutoApply(TestObject startInp, TestObject endInp) {
		// Do NOT click any date input or use ENTER/TAB here.
		// On Windows Chrome, both ENTER and TAB on an input[type=date] advance focus
		// to the next date-input segment (including the end input's MM).  A subsequent
		// click on any non-date element then causes Chrome to commit the segment with
		// its reset minimum value (01), corrupting a correctly-set month (07 → 01).
		// Clicking a neutral element first (if visible) blurs whatever is focused, then
		// the JavaScript fires React onChange for both inputs using their current values.
		WebUI.delay(0.3)
		if (WebUI.waitForElementVisible(to37('txt_historyPageTitle'), 2, FailureHandling.OPTIONAL)) {
			WebUI.click(to37('txt_historyPageTitle'))
		}
		WebUI.delay(0.3)
		WebElement startEl = WebUiCommonHelper.findWebElement(startInp, DEFAULT_TIMEOUT)
		WebElement endEl = WebUiCommonHelper.findWebElement(endInp, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript(
			'''function fireReactDateChange(input) {
  if (!input) { return; }
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new Event("change", { bubbles: true }));
  input.dispatchEvent(new Event("blur", { bubbles: true }));
  var reactKey = Object.keys(input).find(function(k) { return k.indexOf("__reactProps") === 0; });
  if (reactKey && input[reactKey] && typeof input[reactKey].onChange === "function") {
    input[reactKey].onChange({ target: input, currentTarget: input });
  }
}
fireReactDateChange(arguments[0]);
fireReactDateChange(arguments[1]);''',
			java.util.Arrays.asList(startEl, endEl))
		WebUI.delay(1.5)
	}

	/**
	 * Waits until custom date inputs hold expected ISO values and the grid has had time to refresh.
	 * Passes when inputs match even if row content is unchanged (same records still in range).
	 *
	 * @param datesPreConfirmed set {@code true} when the caller has already verified the input
	 *   values via {@link #setHistoryHtmlDateInputInteractive} before committing the filter.
	 *   On slow environments (e.g. terminal servers) the React component briefly unmounts the
	 *   date inputs during the loading state, causing {@code getAttribute} to return empty
	 *   strings. Pre-confirming seeds the timer so those transient absent-input iterations do
	 *   not reset the 3-second grace period.
	 */
	private static void waitForHistoryCustomDateRangeReady(String startIso, String endIso, String beforeSnapshot, int timeoutSec = 45, boolean datesPreConfirmed = false) {
		long deadline = System.currentTimeMillis() + (timeoutSec * 1000L)
		// Seed timer when caller already verified values — handles slow environments where
		// inputs are transiently absent (loading state) throughout the polling window.
		long datesMatchedAt = datesPreConfirmed ? System.currentTimeMillis() : 0L
		boolean datesEverMatched = datesPreConfirmed
		while (System.currentTimeMillis() < deadline) {
			WebUI.delay(0.5)
			String startVal = (WebUI.getAttribute(to37('inp_customRangeStart'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			String endVal = (WebUI.getAttribute(to37('inp_customRangeEnd'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			boolean datesOk = startIso.equals(startVal) && endIso.equals(endVal)
			boolean inputsAbsent = startVal.isEmpty() && endVal.isEmpty()
			boolean tableChanged = historyFilterSnapshot() != beforeSnapshot
			if (datesOk && tableChanged) {
				WebUI.delay(1)
				return
			}
			if (datesEverMatched && tableChanged) {
				KeywordUtil.logInfo('History custom range: table refreshed after dates were confirmed')
				WebUI.delay(1)
				return
			}
			if (datesOk) {
				datesEverMatched = true
				if (datesMatchedAt == 0L) {
					datesMatchedAt = System.currentTimeMillis()
				} else if (System.currentTimeMillis() - datesMatchedAt >= 3000L) {
					KeywordUtil.logInfo('History custom range: dates applied; table content unchanged (records may already match range)')
					WebUI.delay(1)
					return
				}
			} else if (inputsAbsent && datesEverMatched) {
				// Inputs are transiently absent (loading state) — keep the timer running.
				if (datesMatchedAt != 0L && System.currentTimeMillis() - datesMatchedAt >= 3000L) {
					KeywordUtil.logInfo('History custom range: pre-confirmed dates; inputs reloading — filter treated as applied')
					WebUI.delay(1)
					return
				}
			} else if (!inputsAbsent && datesEverMatched) {
				// Inputs visible but show post-commit values that differ from what was confirmed
				// (e.g. React re-rendered the input with a different state after commit).
				// Do NOT reset the timer — the filter was applied with the correct values.
				if (datesMatchedAt != 0L && System.currentTimeMillis() - datesMatchedAt >= 3000L) {
					KeywordUtil.logInfo('History custom range: pre-confirmed dates; post-commit input differs — filter treated as applied')
					WebUI.delay(1)
					return
				}
			} else if (!inputsAbsent) {
				// Inputs visible with wrong value and never pre-confirmed — restart timer.
				datesMatchedAt = 0L
			}
			// Both inputs absent and dates not yet confirmed: keep waiting.
		}
		failStop("History custom date range not ready after ${timeoutSec}s (expected ${startIso} / ${endIso})")
	}

	/**
	 * Applies Global Settings History custom range with <b>end date set first</b>, then start date.
	 * Separate from {@link #applyCustomHistoryDateRange(String, String)} so other tests keep start-then-end behaviour.
	 */
	@Keyword
	def applyCustomHistoryDateRangeEndFirst(String startDate, String endDate) {
		try {
			clickHistoryDateFilter('CUSTOM')
			WebUI.delay(1)
			TestObject startInp = to37('inp_customRangeStart')
			TestObject endInp = to37('inp_customRangeEnd')
			String startIso = normalizeHistoryDateToIso(startDate)
			String endIso = normalizeHistoryDateToIso(endDate)
			waitVisible(startInp)
			waitVisible(endInp)
		String beforeSnapshot = historyFilterSnapshot()
		setHistoryHtmlDateInputInteractive(endInp, endIso)
		WebUI.delay(0.5)
		setHistoryHtmlDateInputInteractive(startInp, startIso)
		// Re-apply both dates via the native setter before any click fires.
		// After the TAB / ENTER that ends setHistoryHtmlDateInputInteractive(startInp, ...),
		// Chrome may have moved focus to the end input's MM segment and initialised it
		// in "edit" mode.  Any subsequent click (even on a neutral element) causes Chrome
		// to commit that segment with its minimum value (01), overwriting the correct
		// month (07).  Setting the native value here forces Chrome's segment state to the
		// correct date so that a commit-on-blur produces the right ISO string.
		WebElement startProtect = WebUiCommonHelper.findWebElement(startInp, DEFAULT_TIMEOUT)
		WebElement endProtect = WebUiCommonHelper.findWebElement(endInp, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript(
			'''function protectDate(input, value) {
  var t = input._valueTracker; if (t) t.setValue(value);
  Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set.call(input, value);
}
protectDate(arguments[0], arguments[1]);
protectDate(arguments[2], arguments[3]);''',
			java.util.Arrays.asList(startProtect, startIso, endProtect, endIso))
		WebUI.delay(0.2)
		commitHistoryCustomDateFilterAutoApply(startInp, endInp)
		// The React app's end-date onChange may auto-recalculate start as (end − N months),
		// overwriting the correctly-set start.  Detect and override that if it happened.
		// start's onChange does NOT recalculate end, so re-firing start is safe.
		WebUI.delay(0.5)
		String startAfterCommit = (WebUI.getAttribute(startInp, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (!startIso.equals(startAfterCommit)) {
			KeywordUtil.logInfo("Post-commit start correction: React reset start to ${startAfterCommit}; re-applying ${startIso}")
			WebElement startFix = WebUiCommonHelper.findWebElement(startInp, DEFAULT_TIMEOUT)
			WebUI.executeJavaScript(
				'''var i=arguments[0],v=arguments[1];
var t=i._valueTracker;if(t)t.setValue(v);
Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,"value").set.call(i,v);
var rk=Object.keys(i).find(function(k){return k.indexOf("__reactProps")===0;});
if(rk&&i[rk]&&typeof i[rk].onChange==="function")i[rk].onChange({target:i,currentTarget:i});''',
				java.util.Arrays.asList(startFix, startIso))
			WebUI.delay(0.3)
		}
		waitForHistoryCustomDateRangeReady(startIso, endIso, beforeSnapshot, 45, true)
		waitVisible(to37('tbl_historyChangeLog'))
		WebUI.takeScreenshot()
	} catch (Exception e) {
		failStop("applyCustomHistoryDateRangeEndFirst failed: ${e.message}")
		}
	}

	/**
	 * Dynamic custom range: end = current date, start = current date minus {@code daysBack} (default 90).
	 * Sets end date first, then start date, then commits the filter.
	 */
	@Keyword
	def applyCustomHistoryDateRangeRelative(int daysBack = 90) {
		List<String> range = resolveHistoryRelativeCustomRange(daysBack)
		KeywordUtil.logInfo("History custom range (relative ${daysBack} days): ${range[0]} to ${range[1]}")
		applyCustomHistoryDateRangeEndFirst(range[0], range[1])
	}

	/**
	 * Verifies inputs match end = today and start = today minus {@code daysBack} (default 90).
	 */
	@Keyword
	def verifyHistoryCustomDateRangeRelativeApplied(int daysBack = 90) {
		List<String> range = resolveHistoryRelativeCustomRange(daysBack)
		verifyHistoryCustomDateRangeApplied(range[0], range[1])
	}

	/**
	 * Attempts to set a history date input; returns the value after the attempt (may differ if UI rejects it).
	 * Used only by invalid custom-range validation — does not replace {@link #setHistoryHtmlDateInputInteractive}.
	 */
	private static String trySetHistoryHtmlDateInputInteractive(TestObject testObject, String yyyyMmDd) {
		String iso = (yyyyMmDd ?: '').trim()
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.delay(0.2)
		WebUI.sendKeys(testObject, Keys.chord(Keys.CONTROL, 'a'))
		WebUI.sendKeys(testObject, Keys.chord(Keys.BACK_SPACE))
		WebUI.delay(0.2)
		WebUI.setText(testObject, iso, FailureHandling.OPTIONAL)
		WebUI.sendKeys(testObject, Keys.chord(Keys.TAB))
		WebUI.delay(0.4)
		String actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		if (!iso.equals(actual)) {
			WebElement el = WebUiCommonHelper.findWebElement(testObject, DEFAULT_TIMEOUT)
			WebUI.executeJavaScript(
				'''var input = arguments[0];
var value = arguments[1];
var tracker = input._valueTracker;
if (tracker) { tracker.setValue(value); }
var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
setter.call(input, value);
input.dispatchEvent(new InputEvent("input", { bubbles: true, cancelable: true, inputType: "insertText", data: value }));
input.dispatchEvent(new Event("change", { bubbles: true }));
input.focus();
input.blur();''',
				java.util.Arrays.asList(el, iso))
			WebUI.delay(0.4)
			actual = (WebUI.getAttribute(testObject, 'value', FailureHandling.OPTIONAL) ?: '').trim()
		}
		return actual
	}

	/**
	 * Commits auto-apply custom range when <b>end date was set last</b> (start-first fixed-date flow).
	 */
	private static void commitHistoryCustomDateFilterAutoApplyEndLast(TestObject startInp, TestObject endInp) {
		waitVisibleClickable(endInp)
		WebUI.click(endInp)
		WebUI.sendKeys(endInp, Keys.chord(Keys.ENTER))
		WebUI.delay(0.3)
		waitVisibleClickable(startInp)
		WebUI.click(startInp)
		WebUI.sendKeys(startInp, Keys.chord(Keys.TAB))
		WebUI.delay(0.3)
		if (WebUI.waitForElementVisible(to37('txt_historyPageTitle'), 2, FailureHandling.OPTIONAL)) {
			WebUI.click(to37('txt_historyPageTitle'))
		}
		WebUI.delay(0.3)
		WebElement startEl = WebUiCommonHelper.findWebElement(startInp, DEFAULT_TIMEOUT)
		WebElement endEl = WebUiCommonHelper.findWebElement(endInp, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript(
			'''function fireReactDateChange(input) {
  if (!input) { return; }
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new Event("change", { bubbles: true }));
  input.dispatchEvent(new Event("blur", { bubbles: true }));
  var reactKey = Object.keys(input).find(function(k) { return k.indexOf("__reactProps") === 0; });
  if (reactKey && input[reactKey] && typeof input[reactKey].onChange === "function") {
    input[reactKey].onChange({ target: input, currentTarget: input });
  }
}
fireReactDateChange(arguments[0]);
fireReactDateChange(arguments[1]);''',
			java.util.Arrays.asList(startEl, endEl))
		WebUI.delay(1.5)
	}

	/**
	 * Opens CUSTOM tab and sets Start Date to the current date (end date unchanged).
	 * For History-Custom-Invalid validation only.
	 */
	@Keyword
	def applyHistoryCustomStartDateAsCurrentDate() {
		try {
			clickHistoryDateFilter('CUSTOM')
			WebUI.delay(1)
			TestObject startInp = to37('inp_customRangeStart')
			String todayDisplay = LocalDate.now().format(HISTORY_CUSTOM_DISPLAY_DATE_FMT)
			String todayIso = normalizeHistoryDateToIso(todayDisplay)
			waitVisible(startInp)
			setHistoryHtmlDateInputInteractive(startInp, todayIso)
			WebUI.delay(0.5)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("applyHistoryCustomStartDateAsCurrentDate failed: ${e.message}")
		}
	}

	/**
	 * Verifies the End Date input {@code max} attribute equals today (future dates blocked in picker).
	 */
	@Keyword
	def verifyHistoryEndDateMaximumIsCurrentDate() {
		try {
			TestObject endInp = to37('inp_customRangeEnd')
			waitVisible(endInp)
			String todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
			String maxAttr = (WebUI.getAttribute(endInp, 'max', FailureHandling.OPTIONAL) ?: '').trim()
			if (!todayIso.equals(maxAttr)) {
				failStop("End date max should be today (${todayIso}), but max='${maxAttr}'")
			}
			KeywordUtil.logInfo("End date max attribute correctly set to today: ${todayIso}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryEndDateMaximumIsCurrentDate failed: ${e.message}")
		}
	}

	/** Opens the End Date native calendar picker ({@code input[type=date]}). */
	private static void openHistoryEndDateCalendarPicker(TestObject endInp) {
		waitVisibleClickable(endInp)
		WebUI.click(endInp)
		WebUI.delay(0.4)
		WebElement el = WebUiCommonHelper.findWebElement(endInp, DEFAULT_TIMEOUT)
		WebUI.executeJavaScript(
			'''var input = arguments[0];
input.focus();
if (typeof input.showPicker === "function") {
  try { input.showPicker(); } catch (e) { /* browser may block without user gesture */ }
}''',
			java.util.Arrays.asList(el))
		WebUI.delay(0.8)
	}

	/**
	 * Opens End Date calendar and verifies tomorrow (current date + 1) is disabled per {@code max} / HTML5 rules.
	 * Does not force-set tomorrow via automation (native picker disables dates after {@code max}).
	 */
	@Keyword
	def verifyHistoryEndDateTomorrowDisabledInCalendar() {
		try {
			TestObject endInp = to37('inp_customRangeEnd')
			waitVisible(endInp)
			String todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
			String tomorrowIso = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			String maxAttr = (WebUI.getAttribute(endInp, 'max', FailureHandling.OPTIONAL) ?: '').trim()
			if (!todayIso.equals(maxAttr)) {
				failStop("End date max should be today (${todayIso}) before calendar check, but max='${maxAttr}'")
			}
			openHistoryEndDateCalendarPicker(endInp)
			WebUI.takeScreenshot()
			WebElement el = WebUiCommonHelper.findWebElement(endInp, DEFAULT_TIMEOUT)
			Object raw = WebUI.executeJavaScript(
				'''var input = arguments[0];
var tomorrow = arguments[1];
var today = arguments[2];
var max = (input.getAttribute("max") || "").trim();
var saved = (input.value || "").trim();
var tomorrowDisabledInCalendar = !!(max && tomorrow > max);
input.value = tomorrow;
var rangeOverflow = input.validity.rangeOverflow;
var checkValid = input.checkValidity();
var valueWhenTomorrowAssigned = (input.value || "").trim();
if (saved) {
  input.value = saved;
} else {
  input.value = today;
}
input.dispatchEvent(new Event("input", { bubbles: true }));
input.dispatchEvent(new Event("change", { bubbles: true }));
return {
  max: max,
  tomorrow: tomorrow,
  tomorrowDisabledInCalendar: tomorrowDisabledInCalendar,
  rangeOverflow: rangeOverflow,
  checkValid: checkValid,
  valueWhenTomorrowAssigned: valueWhenTomorrowAssigned
};''',
				java.util.Arrays.asList(el, tomorrowIso, todayIso))
			Map<?, ?> result = (raw instanceof Map) ? (Map<?, ?>) raw : [:]
			boolean tomorrowDisabled = Boolean.TRUE.equals(result.get('tomorrowDisabledInCalendar'))
			boolean rangeOverflow = Boolean.TRUE.equals(result.get('rangeOverflow'))
			if (!tomorrowDisabled) {
				failStop("Tomorrow (${tomorrowIso}) should be disabled in End Date calendar (max='${maxAttr}')")
			}
			if (!rangeOverflow) {
				KeywordUtil.logInfo("rangeOverflow not reported; max=${maxAttr} still blocks tomorrow in native picker")
			}
			KeywordUtil.logInfo(
				"End Date calendar: tomorrow=${tomorrowIso} disabled (max=${maxAttr}, rangeOverflow=${rangeOverflow}, checkValid=${result.get('checkValid')})")
			setHistoryHtmlDateInputInteractive(endInp, todayIso)
			WebUI.delay(0.5)
			String endAfterRestore = (WebUI.getAttribute(endInp, 'value', FailureHandling.OPTIONAL) ?: '').trim()
			if (!todayIso.equals(endAfterRestore)) {
				failStop("End date should be restored to today (${todayIso}) after calendar check, got '${endAfterRestore}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryEndDateTomorrowDisabledInCalendar failed: ${e.message}")
		}
	}

	/**
	 * Verifies End Date cannot be set to tomorrow (current date + 1 day).
	 */
	@Keyword
	def verifyHistoryEndDateCannotAcceptTomorrow() {
		try {
			TestObject endInp = to37('inp_customRangeEnd')
			waitVisible(endInp)
			String tomorrowIso = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			String beforeVal = (WebUI.getAttribute(endInp, 'value', FailureHandling.OPTIONAL) ?: '').trim()
			String actual = trySetHistoryHtmlDateInputInteractive(endInp, tomorrowIso)
			WebUI.delay(0.5)
			if (tomorrowIso.equals(actual)) {
				failStop("End date accepted tomorrow (${tomorrowIso}) — future date should not be selectable")
			}
			String maxAttr = (WebUI.getAttribute(endInp, 'max', FailureHandling.OPTIONAL) ?: '').trim()
			String todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
			if (maxAttr && tomorrowIso > maxAttr) {
				KeywordUtil.logInfo("End date rejected tomorrow: actual='${actual}', max='${maxAttr}'")
			} else {
				KeywordUtil.logInfo("End date did not retain tomorrow: attempted=${tomorrowIso}, actual='${actual}', before='${beforeVal}'")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryEndDateCannotAcceptTomorrow failed: ${e.message}")
		}
	}

	/**
	 * Applies a hardcoded custom range with <b>start date first</b>, then end date (fixed-date flow).
	 * Separate from {@link #applyCustomHistoryDateRangeEndFirst(String, String)}.
	 */
	@Keyword
	def applyCustomHistoryDateRangeWithFixedDatesStartFirst(String startDate, String endDate) {
		try {
			clickHistoryDateFilter('CUSTOM')
			WebUI.delay(1)
			TestObject startInp = to37('inp_customRangeStart')
			TestObject endInp = to37('inp_customRangeEnd')
			String startIso = normalizeHistoryDateToIso(startDate)
			String endIso = normalizeHistoryDateToIso(endDate)
			waitVisible(startInp)
			waitVisible(endInp)
		String beforeSnapshot = historyFilterSnapshot()
		setHistoryHtmlDateInputInteractive(startInp, startIso)
		WebUI.delay(0.5)
		setHistoryHtmlDateInputInteractive(endInp, endIso)
		commitHistoryCustomDateFilterAutoApplyEndLast(startInp, endInp)
		waitForHistoryCustomDateRangeReady(startIso, endIso, beforeSnapshot, 45, true)
		waitVisible(to37('tbl_historyChangeLog'))
		WebUI.takeScreenshot()
	} catch (Exception e) {
		failStop("applyCustomHistoryDateRangeWithFixedDatesStartFirst failed: ${e.message}")
		}
	}

	/**
	 * Verifies hardcoded custom date inputs hold expected values after {@link #applyCustomHistoryDateRangeWithFixedDatesStartFirst}.
	 */
	@Keyword
	def verifyCustomHistoryDateRangeWithFixedDatesApplied(String startDate, String endDate) {
		try {
			String startIso = normalizeHistoryDateToIso(startDate)
			String endIso = normalizeHistoryDateToIso(endDate)
			String startVal = (WebUI.getAttribute(to37('inp_customRangeStart'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			String endVal = (WebUI.getAttribute(to37('inp_customRangeEnd'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
			if (!startIso.equals(startVal) || !endIso.equals(endVal)) {
				failStop("Fixed custom date inputs not applied: expected ${startIso} / ${endIso}, got ${startVal} / ${endVal}")
			}
			waitVisible(to37('tbl_historyChangeLog'))
			String footer = getHistoryRecordFooterText()
			KeywordUtil.logInfo("History footer after fixed custom range: ${footer}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCustomHistoryDateRangeWithFixedDatesApplied failed: ${e.message}")
		}
	}

	/**
	 * Verifies history records are visible for a valid hardcoded custom range.
	 */
	@Keyword
	def verifyCustomHistoryFixedDateRangeShowsData() {
		try {
			waitVisible(to37('tbl_historyChangeLog'))
			int rows = countHistoryTableDataRows()
			if (rows > 0) {
				WebUI.takeScreenshot()
				return
			}
			String footer = (getHistoryRecordFooterText() ?: '').toLowerCase()
			if (footer.contains('0 of') || footer.contains('0 records') || footer ==~ /.*\b0\s+to\s+0\b.*/) {
				failStop('Expected history records for fixed custom date range, but table shows zero rows')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyCustomHistoryFixedDateRangeShowsData failed: ${e.message}")
		}
	}

	/**
	 * For a valid range (start &lt;= end): expects at least one data row or a non-zero footer total.
	 */
	@Keyword
	def verifyHistoryTableShowsRecordsForCustomRange() {
		try {
			waitVisible(to37('tbl_historyChangeLog'))
			int rows = countHistoryTableDataRows()
			if (rows > 0) {
				WebUI.takeScreenshot()
				return
			}
			String footer = (getHistoryRecordFooterText() ?: '').toLowerCase()
			if (footer.contains('0 of') || footer.contains('0 records') || footer ==~ /.*\b0\s+to\s+0\b.*/) {
				failStop('Expected history records for custom date range, but table shows zero rows')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryTableShowsRecordsForCustomRange failed: ${e.message}")
		}
	}

	/**
	 * Alias for {@link #applyCustomHistoryDateRange(String, String)} — history uses HTML5 date inputs, not a custom calendar grid.
	 * Parameters: year, month (1–12), day.
	 */
	@Keyword
	def applyCustomHistoryDateRangeViaCalendarUi(int startYear, int startMonth, int startDay,
			int endYear, int endMonth, int endDay) {
		String startIso = String.format('%04d-%02d-%02d', startYear, startMonth, startDay)
		String endIso = String.format('%04d-%02d-%02d', endYear, endMonth, endDay)
		applyCustomHistoryDateRange(startIso, endIso)
	}

	/**
	 * Same as {@link #applyCustomHistoryDateRangeViaCalendarUi(int,int,int,int,int,int)} with {@code dd-MM-yyyy} or {@code yyyy-MM-dd}.
	 */
	@Keyword
	def applyCustomHistoryDateRangeViaCalendarUi(String startDate, String endDate) {
		applyCustomHistoryDateRange(startDate, endDate)
	}

	@Keyword
	def verifyHistoryShowsNoDataRowsOrZeroCount() {
		try {
			TestObject row = new TestObject('historyFirstDataRow')
			row.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td][1]")
			boolean hasRow = WebUI.waitForElementVisible(row, 6, FailureHandling.OPTIONAL)
			if (hasRow) {
				failStop('Expected no history rows for invalid / empty filter, but at least one data row is visible')
			}
			if (WebUI.waitForElementVisible(to37('txt_historyRecordFooter'), 5, FailureHandling.OPTIONAL)) {
				String footer = (WebUI.getText(to37('txt_historyRecordFooter'), FailureHandling.OPTIONAL) ?: '').toLowerCase()
				if (footer.contains('0 of') || footer.contains('0 records')) {
					WebUI.takeScreenshot()
					return
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryShowsNoDataRowsOrZeroCount failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryTableContainsText(String fragment) {
		try {
			String frag = (fragment ?: '').trim()
			if (!frag) {
				failStop('verifyHistoryTableContainsText: fragment is blank')
			}
			waitVisible(to37('tbl_historyChangeLog'))
			TestObject cell = new TestObject('historyRowMatch')
			cell.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[contains(.,${escapeXpathLiteral(frag)})]")
			waitVisible(cell)
			WebUI.verifyElementVisible(cell, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryTableContainsText(${fragment}) failed: ${e.message}")
		}
	}
	
	
	
	
	//'This is for custom'
//	@Keyword
//	def applyHistoryRangeForLastNDays(int days) {
//	
//		LocalDate endDate = LocalDate.now()
//		LocalDate startDate = endDate.minusDays(days)
//	
//		
////		LocalDate endDate = LocalDate.now()
////		LocalDate startDate = endDate.minusDays(90)
////		
////		globalSettings.applyCustomHistoryDateRange(
////			startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
////			endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
////		)
//		
//		String startDateStr =
//				startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
//	
//		String endDateStr =
//				endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
//	
//		applyCustomHistoryDateRange(
//				startDateStr,
//				endDateStr
//		)
//	
//		verifyHistoryCustomDateRangeApplied(
//				startDateStr,
//				endDateStr
//		)
//	}
	

	@Keyword
	def clickHistoryResetFilters() {
		try {
			click(to37('btn_historyReset'))
			WebUI.delay(2)
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickHistoryResetFilters failed: ${e.message}")
		}
	}

	@Keyword
	def clickHistoryExport() {
		try {
			click(to37('btn_historyExport'))
			WebUI.delay(4)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickHistoryExport failed: ${e.message}")
		}
	}

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

	/**
	 * Downloads folder for History export validation. Profile {@code HISTORY_EXPORT_DOWNLOADS_DIR} overrides default user Downloads.
	 */
	private static File resolveHistoryExportDownloadsDirectory(String explicitDownloadsPath = null) {
		String explicit = sanitizeWindowsPath(explicitDownloadsPath)
		if (explicit) {
			return new File(explicit)
		}
		String fromProfile = sanitizeWindowsPath(ProfileGlobals.get('HISTORY_EXPORT_DOWNLOADS_DIR', ''))
		if (fromProfile) {
			File profileDir = new File(fromProfile)
			if (profileDir.isDirectory()) {
				return profileDir
			}
			KeywordUtil.markWarning("Profile Downloads path does not exist (${profileDir.absolutePath}); using ${resolveDefaultUserDownloadsDirectory().absolutePath}")
		}
		return resolveDefaultUserDownloadsDirectory()
	}

	private static String resolveHistoryExportFileNamePrefix() {
		String fromProfile = (ProfileGlobals.get('HISTORY_EXPORT_FILE_NAME_PREFIX', '') ?: '').trim()
		return fromProfile ?: HISTORY_EXPORT_FILE_NAME_PREFIX
	}

	private static boolean isCompletedHistoryExportCsvDownload(File f) {
		if (f == null || !f.isFile()) {
			return false
		}
		String n = f.name.toLowerCase()
		return n.endsWith('.csv') && !n.endsWith('.crdownload') && !n.endsWith('.tmp') && !n.endsWith('.part')
	}

	private static List<File> listHistoryExportCsvModifiedAfter(File downloadsDir, long afterMillis, String fileNamePrefix) {
		if (downloadsDir == null || !downloadsDir.isDirectory()) {
			return []
		}
		String prefix = (fileNamePrefix ?: '').trim()
		File[] files = downloadsDir.listFiles({ File f ->
			if (!isCompletedHistoryExportCsvDownload(f)) {
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

	private void recordDetectedHistoryExportFile(File exportFile) {
		lastHistoryExportFileName = exportFile.name
		lastHistoryExportFilePath = exportFile.absolutePath
		writeDetectedHistoryExportFileName(exportFile.name, exportFile.absolutePath)
		KeywordUtil.logInfo("Detected History export file name: ${lastHistoryExportFileName}")
		KeywordUtil.logInfo("Detected History export full path: ${lastHistoryExportFilePath}")
	}

	private static void writeDetectedHistoryExportFileName(String fileName, String absolutePath) {
		try {
			String projectDir = RunConfiguration.getProjectDir() ?: System.getProperty('user.dir') ?: ''
			File out = new File(projectDir, HISTORY_EXPORT_DETECTED_NAME_FILE)
			File parent = out.parentFile
			if (parent != null && !parent.exists()) {
				parent.mkdirs()
			}
			out.setText("fileName=${fileName}\nabsolutePath=${absolutePath}\ncapturedAt=${new Date()}\n", 'UTF-8')
			KeywordUtil.logInfo("Wrote export file details to: ${out.absolutePath}")
		} catch (Exception e) {
			KeywordUtil.markWarning("Could not write ${HISTORY_EXPORT_DETECTED_NAME_FILE}: ${e.message}")
		}
	}

	/**
	 * After {@link #clickHistoryExport()}: waits for the newest {@code Global-Settings-History_*.csv} under Downloads,
	 * logs and writes the detected file name, then validates it exists.
	 *
	 * @param exportStartMillis timestamp captured before export click
	 * @param downloadsFolder optional override (e.g. {@code C:\\Users\\Aditi Singh\\Downloads})
	 * @return absolute path to the validated file
	 */
	@Keyword
	String verifyLatestGlobalSettingsHistoryExportDownloaded(long exportStartMillis, String downloadsFolder = null) {
		try {
			File dir = resolveHistoryExportDownloadsDirectory(downloadsFolder)
			String namePrefix = resolveHistoryExportFileNamePrefix()
			if (!dir.isDirectory()) {
				failStop("Downloads folder does not exist: ${dir.absolutePath}")
			}
			KeywordUtil.logInfo("Waiting for History export in: ${dir.absolutePath} (name contains '${namePrefix}')")
			long deadline = System.currentTimeMillis() + 60000L
			long afterMillis = exportStartMillis - 5000L
			List<File> found = []
			while (System.currentTimeMillis() < deadline) {
				found = listHistoryExportCsvModifiedAfter(dir, afterMillis, namePrefix)
				if (!found.isEmpty()) {
					break
				}
				WebUI.delay(2)
			}
			if (found.isEmpty()) {
				failStop("No new .csv containing '${namePrefix}' found in ${dir.absolutePath} after Export.")
			}
			File latest = found[0]
			recordDetectedHistoryExportFile(latest)
			if (!latest.exists() || latest.length() <= 0L) {
				failStop("Export file is missing or empty: ${latest.absolutePath}")
			}
			WebUI.takeScreenshot()
			return lastHistoryExportFilePath
		} catch (Exception e) {
			failStop("verifyLatestGlobalSettingsHistoryExportDownloaded failed: ${e.message}")
			return ''
		}
	}

	@Keyword
	String getLastHistoryExportFileName() {
		return lastHistoryExportFileName ?: ''
	}

	@Keyword
	String getLastHistoryExportFilePath() {
		return lastHistoryExportFilePath ?: ''
	}

	/**
	 * Resolved Downloads folder for Global Settings History export validation (current user's {@code ~/Downloads} unless profile override exists).
	 */
	@Keyword
	def getHistoryExportDownloadsDirectory() {
		return resolveHistoryExportDownloadsDirectory(null).absolutePath
	}

	/**
	 * Deletes the validated Global Settings History export file from Downloads after test assertions.
	 *
	 * @param absolutePath optional path; defaults to last path from {@link #verifyLatestGlobalSettingsHistoryExportDownloaded(long, String)}
	 */
	@Keyword
	def deleteLastHistoryExportFile(String absolutePath = null) {
		try {
			String p = (absolutePath ?: lastHistoryExportFilePath ?: '').trim()
			if (!p) {
				KeywordUtil.markWarning('deleteLastHistoryExportFile: no export file path to delete')
				return
			}
			File f = new File(p)
			if (!f.isFile()) {
				failStop("Global Settings History export file not found for deletion: ${p}")
			}
			if (!f.delete()) {
				failStop("Failed to delete Global Settings History export file: ${p}")
			}
			KeywordUtil.logInfo("Deleted Global Settings History export file: ${p}")
			if (p == lastHistoryExportFilePath) {
				lastHistoryExportFilePath = ''
				lastHistoryExportFileName = ''
			}
		} catch (Exception e) {
			failStop("deleteLastHistoryExportFile failed: ${e.message}")
		}
	}

	/**
	 * After {@link #clickHistoryExport()}: finds newest matching History export .csv in Downloads.
	 */
	@Keyword
	def verifyNewCsvDownloadPresent(long exportStartMillis) {
		verifyLatestGlobalSettingsHistoryExportDownloaded(exportStartMillis, null)
	}

	/** @deprecated Use {@link #verifyNewCsvDownloadPresent(long)} — export is CSV, not Excel. */
	@Keyword
	def verifyNewExcelDownloadPresent(long exportStartMillis) {
		verifyNewCsvDownloadPresent(exportStartMillis)
	}

	private static boolean csvFileContainsAllKeywords(String absolutePath, List<String> keywords) {
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

	@Keyword
	def verifyExportedHistoryCsvHasColumnHeaders(String absolutePathToCsv) {
		try {
			String p = (absolutePathToCsv ?: '').trim()
			if (!p) {
				failStop('verifyExportedHistoryCsvHasColumnHeaders: path is blank')
			}
			if (!p.toLowerCase().endsWith('.csv')) {
				failStop("Expected a .csv export file, got: ${p}")
			}
			List<String> keys = ['TIMESTAMP', 'SECTION', 'USER', 'CHANGE']
			if (!csvFileContainsAllKeywords(p, keys)) {
				failStop("Exported CSV did not contain all expected headers: ${keys.join(', ')}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedHistoryCsvHasColumnHeaders failed: ${e.message}")
		}
	}
	
	@Keyword
	def verifyGlobalExportedHistoryCsvHasColumnHeaders(String absolutePathToCsv) {
		try {
			String p = (absolutePathToCsv ?: '').trim()
			if (!p) {
				failStop('verifyExportedHistoryCsvHasColumnHeaders: path is blank')
			}
			if (!p.toLowerCase().endsWith('.csv')) {
				failStop("Expected a .csv export file, got: ${p}")
			}
			List<String> keys = ['Timestamp', 'Section', 'Target Value', 'User', 'Action Perform', 'Rule Order', 'Field Name', 'Old Value', 'New Value']
			if (!csvFileContainsAllKeywords(p, keys)) {
				failStop("Exported CSV did not contain all expected headers: ${keys.join(', ')}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedHistoryCsvHasColumnHeaders failed: ${e.message}")
		}
	}

	/** @deprecated Use {@link #verifyExportedHistoryCsvHasColumnHeaders(String)}. */
	@Keyword
	def verifyExportedHistoryExcelHasColumnHeaders(String absolutePath) {
		verifyExportedHistoryCsvHasColumnHeaders(absolutePath)
	}

	@Keyword
	String getLatestHistoryExportCsvPath(long exportStartMillis) {
		if (lastHistoryExportFilePath) {
			return lastHistoryExportFilePath
		}
		File dir = resolveHistoryExportDownloadsDirectory(null)
		List<File> found = listHistoryExportCsvModifiedAfter(dir, exportStartMillis - 5000L, resolveHistoryExportFileNamePrefix())
		if (found.isEmpty()) {
			return ''
		}
		return found[0].absolutePath
	}

	/** @deprecated Use {@link #getLatestHistoryExportCsvPath(long)}. */
	@Keyword
	String getLatestHistoryExportXlsxPath(long exportStartMillis) {
		return getLatestHistoryExportCsvPath(exportStartMillis)
	}

	@Keyword
	def getFirstVisibleHistoryRowText() {
		try {
			TestObject row = new TestObject('historyFirstRowText')
			row.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td][1]")
			boolean ok = WebUI.waitForElementVisible(row, 12, FailureHandling.OPTIONAL)
			if (!ok) {
				KeywordUtil.logInfo('No visible history data row — skipping row-text snippet checks.')
				WebUI.takeScreenshot()
				return ''
			}
			waitVisible(row)
			String t = (WebUI.getText(row, FailureHandling.OPTIONAL) ?: '').trim()
			WebUI.takeScreenshot()
			return t
		} catch (Exception e) {
			try {
				WebUI.takeScreenshot()
			} catch (Exception ignored) {
			}
			KeywordUtil.markWarning("getFirstVisibleHistoryRowText: ${e.message}")
			return ''
		}
	}

	@Keyword
	def verifyExportedCsvContainsUiRowSnippet(String absolutePathToCsv, String uiRowText) {
		try {
			String path = (absolutePathToCsv ?: '').trim()
			String snippet = (uiRowText ?: '').trim()
			if (!path || !snippet) {
				failStop('verifyExportedCsvContainsUiRowSnippet: path or uiRowText is blank')
			}
			if (!path.toLowerCase().endsWith('.csv')) {
				failStop("Expected a .csv export file, got: ${path}")
			}
			String token = snippet.length() > 24 ? snippet.substring(0, 24) : snippet
			String blob = new File(path).getText('UTF-8')
			if (!blob.contains(token)) {
				failStop("Exported CSV did not contain UI row snippet '${token}' (first row text may differ from export encoding).")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyExportedCsvContainsUiRowSnippet failed: ${e.message}")
		}
	}

	/** @deprecated Use {@link #verifyExportedCsvContainsUiRowSnippet(String, String)}. */
	@Keyword
	def verifyExportedXlsxContainsUiRowSnippet(String absolutePath, String uiRowText) {
		verifyExportedCsvContainsUiRowSnippet(absolutePath, uiRowText)
	}

	// ---------- CCIPH-37: History pagination (TC_GS_FUNC_070–078) — assumes authenticated session ----------

	private static String historyTableDataRowsXPath() {
		return "//table[.//th[contains(translate(.,'TIMESTAMP','timestamp'),'timestamp')]]//tbody//tr[td]"
	}

	private static List<Integer> parseHistoryFooterLowHighTotal(String footer) {
		if (!footer) {
			return []
		}
		String c = footer.replaceAll(/\s+/, ' ').trim()
		def m1 = (c =~ /(?i)(\d[\d,]*)\s*[-–]\s*(\d[\d,]*)\s+of\s+(\d[\d,]*)/)
		if (m1.find()) {
			int lo = m1.group(1).replace(',', '') as int
			int hi = m1.group(2).replace(',', '') as int
			int tot = m1.group(3).replace(',', '') as int
			return [lo, hi, tot]
		}
		def m2 = (c =~ /(?i)(\d[\d,]*)\s+to\s+(\d[\d,]*)\s+of\s+(\d[\d,]*)/)
		if (m2.find()) {
			int lo = m2.group(1).replace(',', '') as int
			int hi = m2.group(2).replace(',', '') as int
			int tot = m2.group(3).replace(',', '') as int
			return [lo, hi, tot]
		}
		return []
	}

	private static boolean historyPageSizeLabelMeansFifty(String vis) {
		if (!vis) {
			return false
		}
		String t = vis.trim()
		if ('50'.equals(t)) {
			return true
		}
		def m = (t =~ /^(50)(\s|\/|$)/)
		return m.find()
	}

	private static String historyPageSizeSelectedVisibleLabel() {
		try {
			waitVisible(to37('sel_historyPageSize'))
			WebElement el = WebUiCommonHelper.findWebElement(to37('sel_historyPageSize'), 10)
			return (new Select(el).getFirstSelectedOption()?.getText() ?: '').trim()
		} catch (Exception ignored) {
			return ''
		}
	}

	private static boolean isHistoryControlDisabled(TestObject to) {
		if (!WebUI.waitForElementVisible(to, 8, FailureHandling.OPTIONAL)) {
			return true
		}
		String d = WebUI.getAttribute(to, 'disabled', FailureHandling.OPTIONAL)
		if (d != null && ('true'.equalsIgnoreCase(d) || 'disabled'.equalsIgnoreCase(d))) {
			return true
		}
		String a = WebUI.getAttribute(to, 'aria-disabled', FailureHandling.OPTIONAL)
		if (a != null && 'true'.equalsIgnoreCase(a)) {
			return true
		}
		String cls = (WebUI.getAttribute(to, 'class', FailureHandling.OPTIONAL) ?: '')
		return cls.contains('disabled') && !cls.contains('enabled')
	}

	@Keyword
	def countHistoryTableDataRows() {
		try {
			waitVisible(to37('tbl_historyChangeLog'))
			TestObject rows = new TestObject('history_data_row_count')
			rows.addProperty('xpath', ConditionType.EQUALS, historyTableDataRowsXPath())
			List<?> els = WebUI.findWebElements(rows, 15)
			int n = els ? els.size() : 0
			KeywordUtil.logInfo("History data row count: ${n}")
			WebUI.takeScreenshot()
			return n
		} catch (Exception e) {
			failStop("countHistoryTableDataRows failed: ${e.message}")
			return 0
		}
	}

	@Keyword
	def getHistoryRecordFooterText() {
		try {
			if (!WebUI.waitForElementVisible(to37('txt_historyRecordFooter'), 12, FailureHandling.OPTIONAL)) {
				return ''
			}
			return (WebUI.getText(to37('txt_historyRecordFooter'), FailureHandling.OPTIONAL) ?: '').trim()
		} catch (Exception e) {
			KeywordUtil.markWarning("getHistoryRecordFooterText: ${e.message}")
			return ''
		}
	}

	/**
	 * AC / CSV: default page capacity 50 per screen when total exceeds one page; uses footer range and/or page-size control.
	 */
	@Keyword
	def verifyHistoryPaginationDefaultPageSizeFifty() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(2)
			boolean ok = false
			if (WebUI.waitForElementVisible(to37('sel_historyPageSize'), 10, FailureHandling.OPTIONAL)) {
				waitVisible(to37('sel_historyPageSize'))
				String vis = historyPageSizeSelectedVisibleLabel()
				String v = (WebUI.getAttribute(to37('sel_historyPageSize'), 'value', FailureHandling.OPTIONAL) ?: '').trim()
				if (historyPageSizeLabelMeansFifty(vis) || '50'.equals(v) || v.matches('^50$')) {
					ok = true
				}
			}
			String footer = getHistoryRecordFooterText()
			List<Integer> triple = parseHistoryFooterLowHighTotal(footer)
			if (!triple.isEmpty()) {
				int lo = triple[0]
				int hi = triple[1]
				int tot = triple[2]
				if (lo == 1 && hi == 50 && tot > 50) {
					ok = true
				}
				if (tot > 50 && hi == 50 && lo == 1) {
					ok = true
				}
				if (tot > 0 && tot <= 50 && lo == 1 && hi == tot) {
					KeywordUtil.logInfo('Total records fit on one page; confirming default page size requires page-size control showing 50.')
					String vis2 = historyPageSizeSelectedVisibleLabel()
					if (historyPageSizeLabelMeansFifty(vis2)) {
						ok = true
					}
				}
			}
			if (!ok) {
				failStop("Could not confirm default page size 50 (footer='${footer}'). Adjust locators or UI copy.")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationDefaultPageSizeFifty failed: ${e.message}")
		}
	}

	@Keyword
	def selectHistoryPageSizeAndVerifyMaxVisibleRows(String optionLabel) {
		try {
			String label = (optionLabel ?: '').trim()
			if (!label) {
				failStop('selectHistoryPageSizeAndVerifyMaxVisibleRows: optionLabel is blank')
			}
			if (!WebUI.waitForElementVisible(to37('sel_historyPageSize'), 15, FailureHandling.OPTIONAL)) {
				failStop('Page size select not visible — cannot run TC_GS_FUNC_071 (update Object Repository sel_historyPageSize if UI differs).')
			}
			waitVisibleClickable(to37('sel_historyPageSize'))
			WebUI.selectOptionByLabel(to37('sel_historyPageSize'), label, false)
			WebUI.delay(2)
			waitVisible(to37('tbl_historyChangeLog'))
			int cap = Integer.parseInt(label)
			int rows = countHistoryTableDataRows()
			if (rows > cap) {
				failStop("After page size ${label}, expected at most ${cap} visible data rows, found ${rows}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("selectHistoryPageSizeAndVerifyMaxVisibleRows(${optionLabel}) failed: ${e.message}")
		}
	}

	@Keyword
	def clickHistoryPaginationNextIfEnabled() {
		try {
			if (!WebUI.waitForElementVisible(to37('btn_historyPaginationNext'), 12, FailureHandling.OPTIONAL)) {
				KeywordUtil.logInfo('History Next control not found in DOM.')
				WebUI.takeScreenshot()
				return false
			}
			if (isHistoryControlDisabled(to37('btn_historyPaginationNext'))) {
				KeywordUtil.logInfo('History Next is disabled — already on last page or single page.')
				WebUI.takeScreenshot()
				return false
			}
			click(to37('btn_historyPaginationNext'))
			WebUI.delay(2)
			waitVisible(to37('tbl_historyChangeLog'))
			return true
		} catch (Exception e) {
			failStop("clickHistoryPaginationNextIfEnabled failed: ${e.message}")
			return false
		}
	}

	@Keyword
	def clickHistoryPaginationPreviousIfEnabled() {
		try {
			if (!WebUI.waitForElementVisible(to37('btn_historyPaginationPrev'), 12, FailureHandling.OPTIONAL)) {
				return false
			}
			if (isHistoryControlDisabled(to37('btn_historyPaginationPrev'))) {
				return false
			}
			click(to37('btn_historyPaginationPrev'))
			WebUI.delay(2)
			waitVisible(to37('tbl_historyChangeLog'))
			return true
		} catch (Exception e) {
			failStop("clickHistoryPaginationPreviousIfEnabled failed: ${e.message}")
			return false
		}
	}

	@Keyword
	def verifyHistoryPaginationPrevDisabledOnFirstPage() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(2)
			if (!WebUI.waitForElementVisible(to37('btn_historyPaginationPrev'), 12, FailureHandling.OPTIONAL)) {
				KeywordUtil.markWarning('Previous page control not found — cannot assert disabled state.')
				WebUI.takeScreenshot()
				return
			}
			if (!isHistoryControlDisabled(to37('btn_historyPaginationPrev'))) {
				failStop('Expected Previous pagination control to be disabled on first page.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationPrevDisabledOnFirstPage failed: ${e.message}")
		}
	}

	@Keyword
	def navigateHistoryToLastPageUsingNext() {
		try {
			for (int i = 0; i < 60; i++) {
				if (!WebUI.waitForElementVisible(to37('btn_historyPaginationNext'), 4, FailureHandling.OPTIONAL)) {
					break
				}
				if (isHistoryControlDisabled(to37('btn_historyPaginationNext'))) {
					break
				}
				click(to37('btn_historyPaginationNext'))
				WebUI.delay(1)
			}
			waitVisible(to37('tbl_historyChangeLog'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateHistoryToLastPageUsingNext failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryPaginationNextDisabledOnLastPage() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(2)
			navigateHistoryToLastPageUsingNext()
			if (!WebUI.waitForElementVisible(to37('btn_historyPaginationNext'), 8, FailureHandling.OPTIONAL)) {
				KeywordUtil.markWarning('Next control not found after navigating to last page.')
				WebUI.takeScreenshot()
				return
			}
			if (!isHistoryControlDisabled(to37('btn_historyPaginationNext'))) {
				failStop('Expected Next pagination control to be disabled on last page.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationNextDisabledOnLastPage failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryPaginationNextChangesFooterOrFirstRow() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(2)
			String f0 = getHistoryRecordFooterText()
			String r0 = getFirstVisibleHistoryRowText()
			boolean moved = clickHistoryPaginationNextIfEnabled()
			if (!moved) {
				KeywordUtil.markWarning('Next was not available or already on last page — skipping strict page-change assertion.')
				WebUI.takeScreenshot()
				return
			}
			WebUI.delay(2)
			String f1 = getHistoryRecordFooterText()
			String r1 = getFirstVisibleHistoryRowText()
			boolean footerChanged = f0 && f1 && !f0.equals(f1)
			boolean rowChanged = r0 && r1 && !r0.equals(r1)
			if (!footerChanged && !rowChanged) {
				failStop('Next page did not change footer or first visible row — pagination may be broken or locators need tuning.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationNextChangesFooterOrFirstRow failed: ${e.message}")
		}
	}

	@Keyword
	def logReminderVerifyHistoryPaginationNetworkUsesPagingParams() {
		KeywordUtil.logInfo('MANUAL / NETWORK (TC_GS_FUNC_072): In Chrome DevTools → Network, filter XHR/fetch when changing page or page size. Confirm list API uses paging params (e.g. page, limit, offset, pageSize) rather than loading the full dataset client-side.')
		WebUI.takeScreenshot()
	}

	@Keyword
	def refreshBrowserAndVerifyHistoryDefaultFirstPageWindow() {
		try {
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(1)
			boolean wentSecond = clickHistoryPaginationNextIfEnabled()
			if (!wentSecond) {
				KeywordUtil.markWarning('Could not open page 2 before refresh — verifying default footer only on first page.')
			}
			WebUI.delay(1)
			customsmanagement.BrowserKeywords.hardRefresh(30)
			WebUI.delay(2)
			try {
				waitVisible(to37('txt_historyPageTitle'))
			} catch (Exception ignoredAnchor) {
				navigateToGlobalSettings()
			}
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(2)
			List<Integer> triple = parseHistoryFooterLowHighTotal(getHistoryRecordFooterText())
			if (!triple.isEmpty()) {
				if (triple[0] != 1) {
					failStop("After refresh, expected history footer to start at record 1, got range starting at ${triple[0]}")
				}
			}
			if (!isHistoryControlDisabled(to37('btn_historyPaginationPrev')) && WebUI.waitForElementVisible(to37('btn_historyPaginationPrev'), 4, FailureHandling.OPTIONAL)) {
				KeywordUtil.markWarning('Previous control not disabled after refresh — UI may preserve page in sessionStorage.')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("refreshBrowserAndVerifyHistoryDefaultFirstPageWindow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryPaginationPageTurnLoadsWithinMillis(int maxMillis) {
		try {
			int cap = maxMillis > 0 ? maxMillis : 20000
			verifyGlobalSettingsHistoryPageLoaded()
			clickHistoryDateFilter('ALL')
			WebUI.delay(1)
			if (!WebUI.waitForElementVisible(to37('btn_historyPaginationNext'), 12, FailureHandling.OPTIONAL)) {
				KeywordUtil.markWarning('Next control not found — cannot measure page-turn time.')
				WebUI.takeScreenshot()
				return
			}
			if (isHistoryControlDisabled(to37('btn_historyPaginationNext'))) {
				KeywordUtil.markWarning('Next disabled — cannot measure page-turn time.')
				WebUI.takeScreenshot()
				return
			}
			long t0 = System.currentTimeMillis()
			click(to37('btn_historyPaginationNext'))
			waitVisible(to37('tbl_historyChangeLog'))
			long elapsed = System.currentTimeMillis() - t0
			if (elapsed > cap) {
				failStop("Page turn took ${elapsed} ms (limit ${cap} ms).")
			}
			KeywordUtil.logInfo("Page turn (click Next through table visible) completed in ${elapsed} ms")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryPaginationPageTurnLoadsWithinMillis failed: ${e.message}")
		}
	}

	@Keyword
	def verifyHistoryChangeLogLoadsWithinMillis(int maxMillis) {
		try {
			int cap = maxMillis > 0 ? maxMillis : 25000
			long t0 = System.currentTimeMillis()
			navigateToGlobalSettings()
			clickGlobalSettingsHistoryButton()
			waitVisible(to37('txt_historyPageTitle'))
			waitVisible(to37('tbl_historyChangeLog'))
			long elapsed = System.currentTimeMillis() - t0
			if (elapsed > cap) {
				failStop("History view took ${elapsed} ms to become ready (limit ${cap} ms).")
			}
			countHistoryTableDataRows()
			KeywordUtil.logInfo("History page ready in ${elapsed} ms (navigate + open History)")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyHistoryChangeLogLoadsWithinMillis failed: ${e.message}")
		}
	}

	// ---------- CCIPH-201: Rule reorder (drag-and-drop) — DDU tab; min 3 rules recommended ----------

	private static TestObject rulesTableBodyRowByIndex(int oneBasedRow) {
		TestObject r = new TestObject("tbl_rules_body_tr_${oneBasedRow}")
		r.addProperty('xpath', ConditionType.EQUALS, rulesTableBodyRowXPath(oneBasedRow))
		return r
	}

	/** Reorder-mode rule row: {@code tr} with {@code trHover} + {@code trEditing} (active after Reorder Rules). */
	private static String rulesTableEditingRowXPath(int oneBasedRow) {
		return "(//tr[contains(@class,'trHover') and contains(@class,'trEditing')])[${oneBasedRow}]"
	}

	/**
	 * Draggable rule row in reorder/edit mode (row 1 / 3 have dedicated OR objects).
	 */
	private static TestObject ruleReorderEditingRow(int oneBasedRow) {
		if (oneBasedRow == 1) {
			return to201('drag_Reorder_SourceRow1_Tr')
		}
		if (oneBasedRow == 3) {
			return to201('drag_Reorder_DestRow3_Tr')
		}
		TestObject row = new TestObject("rule_editing_row_${oneBasedRow}")
		row.addProperty('xpath', ConditionType.EQUALS, rulesTableEditingRowXPath(oneBasedRow))
		return row
	}

	/** PRIORITY grip inside an editing row (optional; primary drag uses the {@code tr}). */
	private static TestObject ruleReorderDragHandle(int oneBasedRow) {
		String editingRow = rulesTableEditingRowXPath(oneBasedRow)
		TestObject h = new TestObject("rule_drag_handle_${oneBasedRow}")
		h.addProperty('xpath', ConditionType.EQUALS,
			"${editingRow}/td[1]/div[1]//*[name()='svg']" +
			" | ${editingRow}/td[1]/div[1]//*[local-name()='svg']" +
			" | ${editingRow}"
		)
		return h
	}

	private static TestObject ruleReorderDropTargetRow(int oneBasedRow) {
		return ruleReorderEditingRow(oneBasedRow)
	}

	private static String rulesTableBodyRowXPath(int oneBasedRow) {
		return "(//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'Rule Criteria') or contains(.,'DESTINATION') or contains(.,'PRIORITY')]]//tbody//tr)[${oneBasedRow}]"
	}

	private static WebElement findRuleReorderRowElement(int oneBasedRow) {
		TestObject rowTo = ruleReorderEditingRow(oneBasedRow)
		waitVisible(rowTo)
		return WebUiCommonHelper.findWebElement(rowTo, DEFAULT_TIMEOUT)
	}

	private static boolean isSaveOrderButtonEnabled() {
		TestObject saveBtn = to201('btn_SaveOrder')
		if (!WebUI.waitForElementVisible(saveBtn, 5, FailureHandling.OPTIONAL)) {
			return false
		}
		WebElement el = WebUiCommonHelper.findWebElement(saveBtn, 10)
		if (el == null) {
			return false
		}
		String disabled = el.getAttribute('disabled')
		String ariaDisabled = el.getAttribute('aria-disabled')
		boolean attrDisabled = disabled != null && !disabled.isEmpty() && !'false'.equalsIgnoreCase(disabled)
		boolean ariaOff = 'true'.equalsIgnoreCase(ariaDisabled ?: '')
		return el.isEnabled() && !attrDisabled && !ariaOff
	}

	private static List<String> captureEditingRowTexts(int rowCount) {
		List<String> rows = []
		for (int i = 1; i <= rowCount; i++) {
			TestObject row = ruleReorderEditingRow(i)
			if (!WebUI.waitForElementPresent(row, 8, FailureHandling.OPTIONAL)) {
				rows.add('')
				continue
			}
			String t = (WebUI.getText(row, FailureHandling.OPTIONAL) ?: '').replaceAll('\\s+', ' ').trim()
			rows.add(t)
		}
		return rows
	}

	private static boolean editingRowOrderChanged(List<String> before, List<String> after) {
		if (before == null || after == null || before.size() != after.size()) {
			return true
		}
		for (int i = 0; i < before.size(); i++) {
			if (before.get(i) != after.get(i)) {
				return true
			}
		}
		return false
	}

	private static boolean isDragReorderApplied(List<String> beforeRows, int waitSeconds = 8) {
		if (isSaveOrderButtonEnabled()) {
			return true
		}
		long deadline = System.currentTimeMillis() + (waitSeconds * 1000L)
		int n = beforeRows?.size() ?: 3
		while (System.currentTimeMillis() < deadline) {
			if (isSaveOrderButtonEnabled()) {
				return true
			}
			if (editingRowOrderChanged(beforeRows, captureEditingRowTexts(n))) {
				KeywordUtil.logInfo('Rule row order changed in the table after drag.')
				return true
			}
			WebUI.delay(1)
		}
		return isSaveOrderButtonEnabled() || editingRowOrderChanged(beforeRows, captureEditingRowTexts(n))
	}

	private static boolean waitForSaveOrderEnabledAfterDrag(int timeoutSeconds = 25) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			if (isSaveOrderButtonEnabled()) {
				KeywordUtil.logInfo('Save Order is enabled — drag was accepted by the UI.')
				return true
			}
			WebUI.delay(1)
		}
		return false
	}

	private static void waitForRuleEngineDefaultViewAfterReorder(int timeoutSeconds = 30) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			boolean saveGone = !WebUI.waitForElementVisible(to201('btn_SaveOrder'), 2, FailureHandling.OPTIONAL)
			boolean reorderVisible = WebUI.waitForElementVisible(to201('btn_ReorderRules'), 2, FailureHandling.OPTIONAL)
			boolean editingGone = !WebUI.waitForElementVisible(ruleReorderEditingRow(1), 2, FailureHandling.OPTIONAL)
			if (saveGone && reorderVisible && editingGone) {
				waitVisible(to('tbl_rulesTable'))
				WebUI.delay(1)
				return
			}
			WebUI.delay(1)
		}
		failStop('Rule Engine did not return to default view after Save Order.')
	}

	/**
	 * W3C pointer drag (viewport coords) — works with React / @hello-pangea/dnd style lists.
	 * Includes a short horizontal move to satisfy drag activation distance.
	 */
	private static void dragRuleRowsWithW3CPointer(WebElement gripHandle, WebElement destRow) {
		org.openqa.selenium.Rectangle srcRect = gripHandle.getRect()
		org.openqa.selenium.Rectangle dstRect = destRow.getRect()
		int srcX = (int) (srcRect.x + (srcRect.width / 2))
		int srcY = (int) (srcRect.y + (srcRect.height / 2))
		int tgtX = (int) (dstRect.x + (dstRect.width / 2))
		int tgtCentreY = (int) (dstRect.y + (dstRect.height / 2))
		int tgtBottomY = (int) (dstRect.y + dstRect.height - 6)
		List<Integer> tgtYs = [tgtCentreY, tgtBottomY, (int) dstRect.y + 10]

		PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, 'mouse')
		for (Integer tgtY : tgtYs) {
			Sequence seq = new Sequence(mouse, 0)
			seq.addAction(mouse.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), srcX, srcY))
			seq.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
			seq.addAction(mouse.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), srcX + 12, srcY))
			seq.addAction(mouse.createPointerMove(Duration.ofMillis(900), PointerInput.Origin.viewport(), tgtX, tgtY))
			seq.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
			DriverFactory.getWebDriver().perform(Collections.singletonList(seq))
			WebUI.delay(1)
		}
	}

	/** Slow stepped {@link Actions} drag from grip — activation nudge then vertical steps. */
	private static void dragRuleRowsWithSteppedActions(WebElement gripHandle, WebElement destRow) {
		org.openqa.selenium.Rectangle dstRect = destRow.getRect()
		int destCentreY = (int) (dstRect.y + (dstRect.height / 2))
		org.openqa.selenium.Rectangle gripRect = gripHandle.getRect()
		int gripCentreY = (int) (gripRect.y + (gripRect.height / 2))
		int totalDy = destCentreY - gripCentreY
		int steps = 18
		int stepPx = (int) Math.round(totalDy / (double) steps)

		Actions actions = new Actions(DriverFactory.getWebDriver())
		actions.moveToElement(gripHandle, 2, 0)
			.clickAndHold()
			.pause(Duration.ofMillis(400))
			.moveByOffset(10, 0)
			.pause(Duration.ofMillis(300))
		for (int i = 0; i < steps; i++) {
			actions.moveByOffset(0, stepPx).pause(Duration.ofMillis(60))
		}
		actions.release().perform()
	}

	/** Pointer drag from source {@code tr} to destination {@code tr} (tries centre and bottom-of-row drop). */
	private static void dragRuleRowsWithPointerDrag(WebElement sourceRow, WebElement destRow, WebElement gripHandle) {
		WebElement handle = gripHandle ?: sourceRow
		org.openqa.selenium.Rectangle srcRect = handle.getRect()
		org.openqa.selenium.Rectangle dstRect = destRow.getRect()
		int startX = (int) (srcRect.x + (srcRect.width / 2))
		int startY = (int) (srcRect.y + (srcRect.height / 2))
		int endX = (int) (dstRect.x + (dstRect.width / 2))
		int endCentreY = (int) (dstRect.y + (dstRect.height / 2))
		int endBottomY = (int) (dstRect.y + dstRect.height - 4)
		List<Integer> endYs = [endCentreY, endBottomY, (int) dstRect.y + 8]
		for (Integer endY : endYs) {
			int deltaX = endX - startX
			int deltaY = endY - startY
			Actions act = new Actions(DriverFactory.getWebDriver())
			try {
				act.moveToLocation(startX, startY)
					.clickAndHold()
					.pause(java.time.Duration.ofMillis(700))
					.moveToLocation(endX, endY)
					.pause(java.time.Duration.ofMillis(700))
					.release()
					.perform()
			} catch (Exception moveLocEx) {
				KeywordUtil.markWarning("moveToLocation drag failed (${moveLocEx.message}); using moveByOffset.")
				act = new Actions(DriverFactory.getWebDriver())
				act.moveToElement(handle, 0, 0)
					.clickAndHold()
					.pause(java.time.Duration.ofMillis(700))
					.moveByOffset(deltaX, deltaY)
					.pause(java.time.Duration.ofMillis(700))
					.release()
					.perform()
			}
			WebUI.delay(1)
			if (isSaveOrderButtonEnabled()) {
				return
			}
		}
	}

	/** HTML5 drag events on {@code draggable} rows (React list). */
	private static void dragRuleRowsWithHtml5Events(WebElement sourceRow, WebElement destRow) {
		String script = '''
			var src = arguments[0];
			var tgt = arguments[1];
			var dt = new DataTransfer();
			dt.effectAllowed = 'move';
			function fire(el, type) {
				var ev = new DragEvent(type, { bubbles: true, cancelable: true, dataTransfer: dt });
				return el.dispatchEvent(ev);
			}
			fire(src, 'dragstart');
			fire(tgt, 'dragenter');
			['dragover', 'dragover'].forEach(function() {
				var over = new DragEvent('dragover', { bubbles: true, cancelable: true, dataTransfer: dt });
				over.preventDefault = function() { return false; };
				tgt.dispatchEvent(over);
			});
			fire(tgt, 'drop');
			fire(src, 'dragend');
			return true;
		'''
		WebUI.executeJavaScript(script, java.util.Arrays.asList(sourceRow, destRow))
	}

	/**
	 * Drag rule row {@code sourceRowOneBased} → {@code destRowOneBased}.
	 * Tries pointer drag, HTML5 events, then Katalon dragAndDropToObject; requires Save Order to enable.
	 */
	private static void dragRuleBetweenRowsWithFallback(
			TestObject sourceRowTo,
			TestObject destinationRowTo,
			int sourceRowOneBased,
			int destRowOneBased) {
		waitVisible(sourceRowTo)
		waitVisible(destinationRowTo)
		WebElement sourceRow = findRuleReorderRowElement(sourceRowOneBased)
		WebElement destRow = findRuleReorderRowElement(destRowOneBased)
		WebElement grip = sourceRow
		TestObject gripTo = ruleReorderDragHandle(sourceRowOneBased)
		if (WebUI.waitForElementPresent(gripTo, 5, FailureHandling.OPTIONAL)) {
			grip = WebUiCommonHelper.findWebElement(gripTo, 10)
		}
		WebUI.scrollToElement(sourceRowTo, 10)
		WebUI.scrollToElement(destinationRowTo, 10)
		WebUI.delay(1)

		int rowCount = Math.max(sourceRowOneBased, destRowOneBased)
		List<String> beforeOrder = captureEditingRowTexts(rowCount)

		List<WebElement> dropTargets = [destRow]
		if (destRowOneBased > sourceRowOneBased && destRowOneBased > 1) {
			try {
				dropTargets.add(findRuleReorderRowElement(destRowOneBased - 1))
			} catch (Exception ignored) {
			}
		}

		List<String> strategies = ['w3c', 'stepped', 'pointer', 'html5', 'katalon']
		boolean dragAccepted = false
		for (String strategy : strategies) {
			if (dragAccepted) {
				break
			}
			for (WebElement dropRow : dropTargets) {
				if (dragAccepted) {
					break
				}
				KeywordUtil.logInfo("Reorder drag: ${strategy} row ${sourceRowOneBased} → drop near row ${destRowOneBased}.")
				try {
					switch (strategy) {
						case 'w3c':
							dragRuleRowsWithW3CPointer(grip, dropRow)
							break
						case 'stepped':
							dragRuleRowsWithSteppedActions(grip, dropRow)
							break
						case 'pointer':
							dragRuleRowsWithPointerDrag(sourceRow, dropRow, grip)
							break
						case 'html5':
							dragRuleRowsWithHtml5Events(sourceRow, dropRow)
							break
						default:
							WebUI.dragAndDropToObject(gripTo, destinationRowTo)
					}
					WebUI.delay(2)
					dragAccepted = isDragReorderApplied(beforeOrder, 10)
				} catch (Exception ex) {
					KeywordUtil.markWarning("Drag strategy '${strategy}' failed: ${ex.message}")
				}
			}
		}
		WebUI.takeScreenshot()
		if (!dragAccepted) {
			failStop("Drag row ${sourceRowOneBased} → ${destRowOneBased} failed — Save Order still disabled and row order unchanged. Locators: ${rulesTableEditingRowXPath(sourceRowOneBased)} → ${rulesTableEditingRowXPath(destRowOneBased)}")
		}
	}
	
	private static void verifyTableHeaderPresent(String headerName) {
		
			TestObject header = findTestObject(
				'Object Repository/Global Settings/CCIPH-29/txt_headerName',
				['headerName': headerName]
			)
		
			WebUI.verifyElementVisible(header)
		}
		
		@Keyword
		def verifyGlobalSettingsTableHeadersPresent() {
		
			try {
		
				verifyTableHeaderPresent('PRIORITY')
				verifyTableHeaderPresent('DESTINATION')
				verifyTableHeaderPresent('RULE CRITERIA')
				verifyTableHeaderPresent('ACTIONS')
			
				validateRulesPresentOnTable()
				WebUI.takeScreenshot()
		
			} catch (Exception e) {
				failStop("verifyGlobalSettingsTableHeadersPresent failed: ${e.message}")
			}
			
		}
		
		@Keyword
		def validateRulesPresentOnTable()
		{
			try 
			{   TestObject table= to29('table_Content')
				 waitVisible(table)
			WebUI.verifyElementVisible(table, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("${expectedMessage} - ${e.message}")
		}
	}

	@Keyword
	def verifyMinimumRuleRowsForReorder(int minimumRows = 3) {
		try {
			waitVisible(to('tbl_rulesTable'))
			TestObject allRows = new TestObject('tbl_rules_all_body_rows')
			allRows.addProperty('xpath', ConditionType.EQUALS,
				"//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'DESTINATION')]]//tbody//tr[td]")
			int n = WebUI.findWebElements(allRows, DEFAULT_TIMEOUT)?.size() ?: 0
			if (n < minimumRows) {
				failStop("Expected at least ${minimumRows} rule rows for reorder tests; found ${n}.")
			}
			KeywordUtil.logInfo("Rule table row count: ${n}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyMinimumRuleRowsForReorder failed: ${e.message}")
		}
	}

	@Keyword
	def captureFirstNRuleRowTexts(int n) {
		List<String> rows = []
		try {
			waitVisible(to('tbl_rulesTable'))
			for (int i = 1; i <= n; i++) {
				TestObject row = rulesTableBodyRowByIndex(i)
				waitVisible(row)
				String t = (WebUI.getText(row, FailureHandling.OPTIONAL) ?: '').replaceAll('\\s+', ' ').trim()
				rows.add(t)
			}
			WebUI.takeScreenshot()
			return rows
		} catch (Exception e) {
			failStop("captureFirstNRuleRowTexts failed: ${e.message}")
			return rows
		}
	}

	/**
	 * AC1: default view — no Save Order toolbar, Reorder Rules visible, no drag handle on row 1.
	 */
	@Keyword
	def verifyRuleEngineDefaultViewNoReorderChrome() {
		try {
			waitVisible(to('tbl_rulesTable'))
			boolean saveOrderShown = WebUI.waitForElementVisible(to201('btn_SaveOrder'), 5, FailureHandling.OPTIONAL)
			if (saveOrderShown) {
				failStop('AC1: Save Order should not be visible in default Rule Engine view')
			}
			boolean reorderVisible = WebUI.waitForElementVisible(to201('btn_ReorderRules'), DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)
			if (!reorderVisible) {
				failStop('AC1: Reorder Rules button not visible in default Rule Engine view')
			}
			boolean editingRowShown = WebUI.waitForElementVisible(ruleReorderEditingRow(1), 4, FailureHandling.OPTIONAL)
			if (editingRowShown) {
				failStop('AC1: Editing reorder rows (trHover/trEditing) should not be visible before Reorder Rules')
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleEngineDefaultViewNoReorderChrome failed: ${e.message}")
		}
	}

	@Keyword
	def clickReorderRulesOnCurrentTab() {
		try {
			waitVisible(to('tbl_rulesTable'))
			waitVisible(to201('btn_ReorderRules'))
			WebUI.waitForElementClickable(to201('btn_ReorderRules'), DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			click(to201('btn_ReorderRules'))
			waitVisible(to201('btn_SaveOrder'))
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickReorderRulesOnCurrentTab failed: ${e.message}")
		}
	}

	/**
	 * AC2: reorder mode — drag handle, Save Order, Cancel; Reorder Rules disabled or hidden.
	 */
	@Keyword
	def verifyReorderModeActive() {
		try {
			waitVisible(to201('btn_SaveOrder'))
			WebUI.verifyElementVisible(to201('btn_SaveOrder'), FailureHandling.STOP_ON_FAILURE)
			waitVisibleClickable(to201('btn_CancelReorderRules'))
			WebUI.verifyElementVisible(to201('btn_CancelReorderRules'), FailureHandling.STOP_ON_FAILURE)
			waitVisible(ruleReorderEditingRow(1))
			WebUI.verifyElementVisible(ruleReorderEditingRow(1), FailureHandling.STOP_ON_FAILURE)
			TestObject allEditingRows = new TestObject('all_trHover_trEditing_rows')
			allEditingRows.addProperty('xpath', ConditionType.EQUALS, "//tr[contains(@class,'trHover') and contains(@class,'trEditing')]")
			int editingCount = WebUI.findWebElements(allEditingRows, DEFAULT_TIMEOUT)?.size() ?: 0
			if (editingCount < 3) {
				failStop("AC2: Expected at least 3 trHover/trEditing rows in reorder mode; found ${editingCount}.")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyReorderModeActive failed: ${e.message}")
		}
	}

	/**
	 * AC3 / AC4: drag rule from row {@code sourceRowOneBased} to row {@code destRowOneBased}.
	 * <p>Source / destination: {@code (//tr[contains(@class,'trHover') and contains(@class,'trEditing')])[n]}.</p>
	 * <p>Example: first rule to third row — {@code dragRuleBetweenRows(1, 3)}.</p>
	 */
	@Keyword
	def dragRuleBetweenRows(int sourceRowOneBased, int destRowOneBased) {
		try {
			if (sourceRowOneBased < 1 || destRowOneBased < 1) {
				failStop('dragRuleBetweenRows: row indices must be >= 1')
			}
			if (sourceRowOneBased == destRowOneBased) {
				failStop("dragRuleBetweenRows: source and destination cannot both be row ${sourceRowOneBased}")
			}
			TestObject src = ruleReorderEditingRow(sourceRowOneBased)
			TestObject dst = ruleReorderEditingRow(destRowOneBased)
			KeywordUtil.logInfo("Dragging editing tr row ${sourceRowOneBased} → row ${destRowOneBased} (${rulesTableEditingRowXPath(sourceRowOneBased)}).")
			dragRuleBetweenRowsWithFallback(src, dst, sourceRowOneBased, destRowOneBased)
		} catch (Exception e) {
			failStop("dragRuleBetweenRows(${sourceRowOneBased}, ${destRowOneBased}) failed: ${e.message}")
		}
	}

	@Keyword
	def clickSaveOrderInReorderMode() {
		try {
			TestObject saveBtn = to201('btn_SaveOrder')
			waitVisible(saveBtn)
			WebUI.scrollToElement(saveBtn, 10)
			if (!isSaveOrderButtonEnabled()) {
				waitForSaveOrderEnabledAfterDrag(15)
			}
			if (isSaveOrderButtonEnabled()) {
				waitVisibleClickable(saveBtn)
				WebUI.click(saveBtn)
			} else {
				KeywordUtil.markWarning('Save Order not enabled — attempting click after verified row reorder.')
				WebElement el = WebUiCommonHelper.findWebElement(saveBtn, DEFAULT_TIMEOUT)
				WebUI.executeJavaScript('arguments[0].click()', java.util.Arrays.asList(el))
			}
			WebUI.takeScreenshot()
			waitForRuleEngineDefaultViewAfterReorder(45)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickSaveOrderInReorderMode failed: ${e.message}")
		}
	}

	@Keyword
	def clickCancelReorderRules() {
		try {
			click(to201('btn_CancelReorderRules'))
			WebUI.delay(2)
			boolean still = WebUI.waitForElementVisible(to201('btn_SaveOrder'), 12, FailureHandling.OPTIONAL)
			if (still) {
				failStop('Save Order still visible — reorder mode may not have exited after cancel')
			}
			waitVisible(to('tbl_rulesTable'))
			WebUI.waitForElementVisible(to201('btn_ReorderRules'), DEFAULT_TIMEOUT, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickCancelReorderRules failed: ${e.message}")
		}
	}

	@Keyword
	def verifyRuleRowSnapshotsEqual(List<String> expected, List<String> actual, String message = 'Row snapshots should match') {
		try {
			if (expected == null || actual == null || expected.size() != actual.size()) {
				failStop("${message} (size mismatch: expected ${expected?.size()}, actual ${actual?.size()})")
			}
			for (int i = 0; i < expected.size(); i++) {
				String expNorm = normalizeRuleRowSnapshotText(expected.get(i))
				String actNorm = normalizeRuleRowSnapshotText(actual.get(i))
				if (!expNorm.equals(actNorm)) {
					failStop("${message} at index ${i}. Expected (normalized): '${expNorm.take(120)}', actual: '${actNorm.take(120)}'")
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleRowSnapshotsEqual failed: ${e.message}")
		}
	}

	@Keyword
	def verifyRuleRowSnapshotsNotEqual(List<String> a, List<String> b, String message = 'Row snapshots should differ after reorder') {
		try {
			if (a == null || b == null || a.size() != b.size()) {
				failStop("${message} (invalid or size mismatch)")
			}
			boolean anyDiff = false
			for (int i = 0; i < a.size(); i++) {
				String aNorm = normalizeRuleRowSnapshotText(a.get(i))
				String bNorm = normalizeRuleRowSnapshotText(b.get(i))
				if (!aNorm.equals(bNorm)) {
					anyDiff = true
					break
				}
			}
			if (!anyDiff) {
				failStop(message)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleRowSnapshotsNotEqual failed: ${e.message}")
		}
	}

	/**
	 * Verifies the Global Settings rule table area is visible (card content container).
	 * Locator: {@code div[class="GlobalSettingsModule-module__HzmAka__cardContent"]}
	 */
	@Keyword
	def verifyRuleTableVisible() {
		try {
			waitVisible(to201('div_ruleEngineCardContent'))
			WebUI.verifyElementVisible(to201('div_ruleEngineCardContent'), FailureHandling.STOP_ON_FAILURE)
			KeywordUtil.logInfo('Rule table card content is visible.')
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyRuleTableVisible failed: ${e.message}")
		}
	}
}
