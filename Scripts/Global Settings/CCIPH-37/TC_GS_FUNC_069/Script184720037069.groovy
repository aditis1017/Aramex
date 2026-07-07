import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_069 — export aligns with UI

long exportStart = System.currentTimeMillis()
def login = new MSLogin()
def globalSettings = new GS()

login.loginToCustomsManagementUsingAutomationProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()
String rowText = globalSettings.getFirstVisibleHistoryRowText()
globalSettings.clickHistoryExport()
globalSettings.verifyNewCsvDownloadPresent(exportStart)
String path = globalSettings.getLatestHistoryExportCsvPath(exportStart)
globalSettings.verifyGlobalExportedHistoryCsvHasColumnHeaders(path)
if (rowText) {
	globalSettings.verifyExportedCsvContainsUiRowSnippet(path, rowText)
}
if (path) {
	if (globalSettings.respondsTo('deleteLastHistoryExportFile', path)) {
		globalSettings.deleteLastHistoryExportFile(path)
	} else {
		File exportFile = new File(path)
		if (exportFile.isFile()) {
			exportFile.delete()
			KeywordUtil.logInfo("Deleted Global Settings History export file: ${path}")
		}
	}
}

WebUI.takeScreenshot()
WebUI.closeBrowser()

// Issue raised for the Date / Time format
