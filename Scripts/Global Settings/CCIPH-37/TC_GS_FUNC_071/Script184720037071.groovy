import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_071 — page size 10 / 20

def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()
globalSettings.clickHistoryDateFilter('ALL')
globalSettings.selectHistoryPageSizeAndVerifyMaxVisibleRows('10')
globalSettings.selectHistoryPageSizeAndVerifyMaxVisibleRows('20')
globalSettings.selectHistoryPageSizeAndVerifyMaxVisibleRows('50')
WebUI.takeScreenshot()

// Deprecated
WebUI.closeBrowser()
//need to remove as Pagination removed from scope