import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_064 — rule update in history

String newVal = String.valueOf(System.currentTimeMillis() % 100000)
def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingProfile()
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickEditFirstRuleContainingCostOfGoods()
globalSettings.verifyRuleModalVisible()
globalSettings.enterConditionValue(newVal)
globalSettings.saveRuleModal()
globalSettings.dismissRuleUpdateSuccessModalIfPresent()
globalSettings.clickGlobalSettingsHistoryButton()
globalSettings.verifyGlobalSettingsHistoryPageLoaded()
globalSettings.clickHistoryDateFilter(history_Filter)
globalSettings.verifyHistoryTableContainsText(newVal)
WebUI.takeScreenshot()
WebUI.closeBrowser()
