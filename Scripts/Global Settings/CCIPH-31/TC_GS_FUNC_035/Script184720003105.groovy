import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_035

	def login = new MSLogin()
	def globalSettings = new GS()
	def updatedVal = String.valueOf(System.currentTimeMillis() % 100000)
	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
	login.loginToCustomsManagementUsingProfile()
	'This is used for Navigating to Management'

	globalSettings.navigateToGlobalSettings()
	WebUI.delay(2)
	globalSettings.openTab(tab)
	globalSettings.clickEditFirstRuleContainingCostOfGoods()
	globalSettings.verifyRuleModalVisible()
	globalSettings.verifyConditionValueFieldNotEmpty()	
	globalSettings.verifyRuleModalVisible()

	globalSettings.enterConditionValue(updatedVal)
	globalSettings.saveRuleModal()
	globalSettings.dismissRuleUpdateSuccessModalIfPresent()
	globalSettings.verifySavedRuleWithCondition(conditionField, updatedVal, conditionOperator, destination)
//	globalSettings.verifyRuleRowDetails(updatedVal, updatedVal)
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
