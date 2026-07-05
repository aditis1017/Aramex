import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_054 | Jira: CCIPH-37
// Preconditions: Super Admin session via profile (Microsoft login).

	def login = new MSLogin()
	def globalSettings = new GS()

	login.loginToCustomsManagementUsingAutomationProfile()
	globalSettings.navigateToGlobalSettings()
	globalSettings.clickGlobalSettingsHistoryButton()
	globalSettings.verifyGlobalSettingsHistoryPageLoaded()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
