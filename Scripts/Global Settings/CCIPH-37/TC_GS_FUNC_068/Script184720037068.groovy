import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_068 — exported file columns

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()
globalSettings.clickHistoryExport()
globalSettings.verifyNewCsvDownloadPresent(exportStart)
String path = globalSettings.getLatestHistoryExportCsvPath(exportStart)
globalSettings.verifyExportedHistoryCsvHasColumnHeaders(path)
WebUI.takeScreenshot()

// Rework
WebUI.closeBrowser()
