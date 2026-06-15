package globalsettings

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import internal.GlobalVariable

/**
 * Shared business actions for Global Settings (CCIPH-30).
 * Assumes authenticated session — login is handled separately.
 */
class BusinessActions {

	private static final String OR_PREFIX = 'Global Settings/CCIPH-30/'
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

	private static void click(TestObject testObject) {
		waitVisibleClickable(testObject)
		WebUI.click(testObject)
		WebUI.takeScreenshot()
	}

	@Keyword
	def navigateToGlobalSettings() {
		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			String url = GlobalVariable.GLOBAL_SETTINGS_URL ?: GlobalVariable.URL
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
			click(to('btn_modalSave'))
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
			WebUI.delay(2)
			boolean duplicate = WebUI.waitForElementVisible(to('msg_duplicateRule'), 5, FailureHandling.OPTIONAL)
			if (!duplicate) {
				waitVisible(to('txt_anchor_GlobalSettings'))
			}
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

	private static TestObject ruleTableRowContaining(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'DESTINATION')]]//tbody//tr[${rowCondition}]" +
			" | //table//tbody//tr[${rowCondition}]"

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
			waitVisible(to('tbl_rulesTable'))
			TestObject ruleRow = ruleTableRowContaining(expectedTexts as List)
			waitVisible(ruleRow)
			WebUI.verifyElementVisible(ruleRow, FailureHandling.STOP_ON_FAILURE)
			KeywordUtil.logInfo("Saved rule found in table matching: ${(expectedTexts as List).join(', ')}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifySavedRuleInTable failed: ${e.message}")
		}
	}

	/**
	 * Verifies a saved rule by structured condition parts (field, operator, value, optional destination).
	 * Reusable for DDU / DDP / Category tabs.
	 *
	 * Example (TC_GS_FUNC_014 style):
	 *   verifySavedRuleWithCondition('Cost of Goods', '47810', 'Greater than', 'Saudi Arabia')
	 */
	@Keyword
	def verifySavedRuleWithCondition(String fieldName, String conditionValue, String operator = 'Greater than', String destination = null) {
		List<String> parts = []
		if ((destination ?: '').trim()) {
			parts << destination.trim()
		}
		parts << (fieldName ?: '').trim()
		parts << (operator ?: '').trim()
		parts << (conditionValue ?: '').trim()
		verifySavedRuleInTable(parts as String[])
	}

	private static String buildRuleRowMatchCondition(List<String> expectedTexts) {
		List<String> fragments = (expectedTexts ?: []).collect { (it ?: '').trim() }.findAll { it }
		if (fragments.isEmpty()) {
			failStop('Rule row match requires at least one expected text fragment')
		}
		return fragments.collect { fragment ->
			"contains(normalize-space(.), ${escapeXpathLiteral(fragment)})"
		}.join(' and ')
	}

	private static TestObject ruleTableRowEditButton(List<String> expectedTexts) {
		String rowCondition = buildRuleRowMatchCondition(expectedTexts)
		String xpath = "//table[.//th[contains(.,'RULE CRITERIA') or contains(.,'DESTINATION')]]//tbody//tr[${rowCondition}]" +
			"//*[self::button or self::a][contains(.,'Edit') or contains(@aria-label,'Edit') or contains(@title,'Edit')]" +
			" | //table//tbody//tr[${rowCondition}]//*[self::button or self::a][contains(.,'Edit') or contains(@aria-label,'Edit') or contains(@title,'Edit')]"

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
}
