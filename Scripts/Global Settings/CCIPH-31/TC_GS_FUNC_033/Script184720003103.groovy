import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_033

	def login = new MSLogin()
	def globalSettings = new GS()
	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
	login.loginToCustomsManagementUsingProfile()
	'This is used for Navigating to Management'
	globalSettings.navigateToGlobalSettings()
	WebUI.delay(3)
	globalSettings.openTab(tab)
	globalSettings.clickEditFirstRule()
	globalSettings.verifyRuleModalVisible()
//	globalSettings.verifyConditionValueFieldNotEmpty()
//	globalSettings.openTab(tab2)
//	globalSettings.clickEditFirstRule()
//	globalSettings.verifyRuleModalVisible()
//	globalSettings.verifyConditionValueFieldNotEmpty()
//	WebUI.takeScreenshot()
//	WebUI.closeBrowser()
//Need to check