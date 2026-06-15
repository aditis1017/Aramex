import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_063 — rule deactivation in history

def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingProfile()
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickDeactivateFirstRule()
globalSettings.verifyDeactivateConfirmationVisible()
globalSettings.confirmDeactivateRule()
globalSettings.clickGlobalSettingsHistoryButton()
globalSettings.verifyGlobalSettingsHistoryPageLoaded()
globalSettings.clickHistoryDateFilter(history_Filter)
globalSettings.verifyHistoryTableContainsText(rule_Status)
WebUI.takeScreenshot()

//Rework, Need to remove tc
WebUI.closeBrowser()
