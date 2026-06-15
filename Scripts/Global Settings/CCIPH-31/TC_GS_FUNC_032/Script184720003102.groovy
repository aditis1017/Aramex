import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS
// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_032

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
	globalSettings.clickModalCancel()
	globalSettings.openTab(tab2)
	globalSettings.clickEditFirstRule()
	globalSettings.clickModalCancel()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
