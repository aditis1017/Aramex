//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//
//import globalsettings.BusinessActions as GS
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_037 — creates two rules then edits first to duplicate second.
//
//def v1 = String.valueOf((System.nanoTime() % 400000) + 100000)
//def v2 = String.valueOf((System.nanoTime() % 400000) + 500000)
//
//def globalSettings = new GS()
//globalSettings.cciph31_editFirstRuleToDuplicateSecondRuleOnDdu(v1, v2)
//WebUI.takeScreenshot()

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

	def login = new MSLogin()
	def globalSettings = new GS()
	def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
    login.loginToCustomsManagementUsingAutomationProfile()
   //login.loginToStageCustomsManagementUsingAutomationProfile()
    'This is used for Navigating to Management'
    globalSettings.navigateToGlobalSettings()
   //globalSettings.navigateToStageGlobalSettings()


	globalSettings.createDduRuleWithCostOfGoods(ruleValue)
	
	globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, conditionOperator)

	WebUI.delay(1)
	globalSettings.createDduRuleWithCostOfGoods(ruleValue)
	//globalSettings.selectConditionFieldOperatorAndValueByLabel(conditionField, conditionOperator, valuesComparator)
	//globalSettings.clickModalSave()
	globalSettings.verifyDuplicateRuleMessageVisible()

	WebUI.takeScreenshot()
	WebUI.closeBrowser()
	
	// rework
