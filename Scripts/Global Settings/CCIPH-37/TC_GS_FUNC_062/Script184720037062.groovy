import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_062 — rule creation in history (ALL)

String uniqueVal = String.valueOf(System.currentTimeMillis() % 100000)
def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingAutomationProfile()
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.createDduRuleWithCostOfGoods(uniqueVal)
globalSettings.clickGlobalSettingsHistoryButton()
globalSettings.verifyGlobalSettingsHistoryPageLoaded()
globalSettings.clickHistoryDateFilter(history_Filter)
globalSettings.verifyHistoryTableContainsText(uniqueVal)
WebUI.takeScreenshot()
WebUI.closeBrowser()
