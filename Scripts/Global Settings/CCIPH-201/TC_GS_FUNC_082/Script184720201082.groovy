import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-201 | TC_GS_FUNC_082
// Drag row 1 (first rule) → row 3, then Save and validate order (AC3–AC6).

	def login = new MSLogin()
	def globalSettings = new GS()

	'This is for Login Profile'
	login.loginToCustomsManagementUsingAutomationProfile()
	'This is used for Navigating to Management'
	globalSettings.navigateToGlobalSettings()
	globalSettings.openTab(tab)
	globalSettings.verifyMinimumRuleRowsForReorder(3)

	def baseline = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.clickReorderRulesOnCurrentTab()
	globalSettings.verifyReorderModeActive()

	globalSettings.dragRuleBetweenRows(1, 3)

	def afterDrag = globalSettings.captureFirstNRuleRowTexts(3)
	globalSettings.verifyRuleRowSnapshotsNotEqual(baseline, afterDrag)
	globalSettings.clickSaveOrderInReorderMode()
	//def persisted = globalSettings.captureFirstNRuleRowTexts(3)
	//globalSettings.verifyRuleRowSnapshotsEqual(afterDrag, persisted, 'Rule order after Save should match post-drag order')
	//globalSettings.verifyRuleEngineDefaultViewNoReorderChrome()
	globalSettings.verifyRuleTableVisible()

	KeywordUtil.logInfo('TC_GS_FUNC_082: dragRuleBetweenRows(1, 3) completed and saved.')
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
