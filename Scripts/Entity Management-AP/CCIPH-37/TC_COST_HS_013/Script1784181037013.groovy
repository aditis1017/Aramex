// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_013
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

long exportStart = System.currentTimeMillis()
def em = new EM()
def gs = new GS()
def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
String rowText = gs.getFirstVisibleHistoryRowText()
gs.clickHistoryExport()
gs.verifyNewCsvDownloadPresent(exportStart)
String path = gs.getLatestHistoryExportCsvPath(exportStart)
gs.verifyGlobalExportedHistoryCsvHasColumnHeaders(path)
if (rowText?.trim()) {
	gs.verifyExportedCsvContainsUiRowSnippet(path, rowText)
}
if (path && gs.respondsTo('deleteLastHistoryExportFile', path)) {
	gs.deleteLastHistoryExportFile(path)
}
WebUI.takeScreenshot()
WebUI.closeBrowser()
