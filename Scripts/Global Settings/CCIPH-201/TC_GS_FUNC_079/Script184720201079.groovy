import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-201 | TC_GS_FUNC_079 | AC1 + AC2 entry

	def login = new MSLogin()
	def globalSettings = new GS()

	'This is for Login Profile'
	login.loginToCustomsManagementUsingAutomationProfile()
	'This is used for Navigating to Management'
	globalSettings.navigateToGlobalSettings()
	globalSettings.openTab(tab)
	globalSettings.verifyMinimumRuleRowsForReorder(3)
	globalSettings.verifyRuleEngineDefaultViewNoReorderChrome()
	globalSettings.clickReorderRulesOnCurrentTab()
	globalSettings.verifyReorderModeActive()
	KeywordUtil.logInfo('CCIPH-201: reorder mode active — drag handles and Save Order / Cancel visible.')
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
