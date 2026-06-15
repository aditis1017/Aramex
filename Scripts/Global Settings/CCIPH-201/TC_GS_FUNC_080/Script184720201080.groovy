import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-201 | TC_GS_FUNC_080 | AC6 Cancel

	def login = new MSLogin()
	def globalSettings = new GS()

	'This is for Login Profile'
	login.loginToCustomsManagementUsingProfile()
	'This is used for Navigating to Management'
	globalSettings.navigateToGlobalSettings()
	globalSettings.openTab(tab)
	globalSettings.verifyMinimumRuleRowsForReorder(3)

	def before = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.clickReorderRulesOnCurrentTab()
	globalSettings.verifyReorderModeActive()
	globalSettings.dragRuleBetweenRows(1, 3)
	def mid = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.verifyRuleRowSnapshotsNotEqual(before, mid)
	globalSettings.clickCancelReorderRules()
	def after = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.verifyRuleRowSnapshotsEqual(before, after, 'After Cancel, rule order should match pre-reorder snapshot')
	globalSettings.verifyRuleEngineDefaultViewNoReorderChrome()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
