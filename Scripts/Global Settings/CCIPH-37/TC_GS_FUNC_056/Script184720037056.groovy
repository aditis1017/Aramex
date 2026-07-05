import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_056 — AC1 table columns

	def login = new MSLogin()
	def globalSettings = new GS()

	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
    login.loginToCustomsManagementUsingAutomationProfile()
 //  login.loginToStageCustomsManagementUsingAutomationProfile()
    'This is used for Navigating to Management'
   globalSettings.navigateToGlobalSettings()
   //globalSettings.navigateToStageGlobalSettings()
   globalSettings.clickGlobalSettingsHistoryButton()
   globalSettings.verifyGlobalSettingsHistoryPageLoaded()
   globalSettings.verifyHistoryLogTableColumnHeadersPresent()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
