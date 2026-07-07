import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-201 | TC_GS_FUNC_081 | AC3 + AC4 + AC5 Save

	def login = new MSLogin()
	def globalSettings = new GS()

	'This is for Login Profile'
	login.loginToCustomsManagementUsingAutomationProfile()
	//login.loginToStageCustomsManagementUsingAutomationProfile()
	'This is used for Navigating to Management'
	globalSettings.navigateToGlobalSettings()
	//globalSettings.navigateToStageGlobalSettings()
	globalSettings.openTab(tab)
	globalSettings.verifyMinimumRuleRowsForReorder(3)

	def before = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.clickReorderRulesOnCurrentTab()
	globalSettings.verifyReorderModeActive()
	globalSettings.dragRuleBetweenRows(2, 1)
	def mid = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.verifyRuleRowSnapshotsNotEqual(before, mid)
	globalSettings.clickSaveOrderInReorderMode()
	def after = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.verifyRuleRowSnapshotsEqual(mid, after, 'Persisted table should match order immediately before Save')
	globalSettings.verifyRuleEngineDefaultViewNoReorderChrome()
	KeywordUtil.logInfo('CCIPH-201: sequence saved; reorder mode exited (AC5).')
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
