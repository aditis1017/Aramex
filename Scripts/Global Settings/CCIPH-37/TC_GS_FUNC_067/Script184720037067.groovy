import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_067 — Export Excel to Downloads

String downloadsDir = 'C:\\Users\\Aditi Singh\\Downloads'
long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()
globalSettings.clickHistoryExport()

String exportPath = globalSettings.verifyLatestGlobalSettingsHistoryExportDownloaded(exportStart, downloadsDir)
KeywordUtil.logInfo("Validated latest export — file name: ${globalSettings.getLastHistoryExportFileName()}")
KeywordUtil.logInfo("Validated latest export — full path: ${exportPath}")

WebUI.takeScreenshot()
WebUI.closeBrowser()
