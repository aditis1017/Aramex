import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_036 | Logout/login steps omitted per AUTOMATION-PROMPT-STABLE-REUSABLE (login isolation).

	def login = new MSLogin()
	def globalSettings = new GS()
	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
	login.loginToCustomsManagementUsingProfile()
	'This is used for Navigating to Management'

	globalSettings.navigateToGlobalSettings()
	WebUI.delay(2)
	globalSettings.openTab(tab)
	globalSettings.clickEditFirstRule()
	globalSettings.verifyRuleModalVisible()
	globalSettings.verifyConditionValueFieldNotEmpty()
	globalSettings.enterConditionValue(persistedVal)
	globalSettings.saveRuleModal()
	globalSettings.dismissRuleUpdateSuccessModalIfPresent()
	globalSettings.refreshGlobalSettingsPage()
	globalSettings.openTab(tab)
	globalSettings.verifySavedRuleWithCondition(conditionField, persistedVal, conditionOperator, destination)
	WebUI.takeScreenshot()

	// DUPLICATE
	WebUI.closeBrowser()
//will remove as this TC already covered