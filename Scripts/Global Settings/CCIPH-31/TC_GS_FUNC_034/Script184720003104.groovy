import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_034

	def login = new MSLogin()
	def globalSettings = new GS()
	
	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
   //login.loginToCustomsManagementUsingAutomationProfile()
    login.loginToStageCustomsManagementUsingAutomationProfile()
    'This is used for Navigating to Management'
   //globalSettings.navigateToGlobalSettings()
    globalSettings.navigateToStageGlobalSettings()
	//WebUI.delay(2)
	globalSettings.openTab(tab)
	globalSettings.clickEditFirstRule()
	globalSettings.verifyRuleModalVisible()
//	globalSettings.verifyConditionValueFieldNotEmpty()
//	globalSettings.verifyFirstConditionRowValuePresent()
	globalSettings.clickModalCancel()
	globalSettings.verifyModalNotVisible()
	globalSettings.openTab(tab2)
	globalSettings.clickEditFirstRule()
	globalSettings.verifyRuleModalVisible()
	//globalSettings.verifyConditionValueFieldNotEmpty()
//	globalSettings.verifyFirstConditionRowValuePresent()
	globalSettings.clickModalCancel()
	globalSettings.verifyModalNotVisible()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
