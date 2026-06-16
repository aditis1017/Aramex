import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_058 — TODAY filter

	def login = new MSLogin()
	def globalSettings = new GS()

	login.loginToCustomsManagementUsingProfile()
	globalSettings.openGlobalSettingsHistoryFromLanding()
	globalSettings.clickHistoryDateFilter(history_Filter)
	globalSettings.verifyHistoryTableStillDisplayed()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
