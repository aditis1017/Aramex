// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_013
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import com.kms.katalon.core.util.KeywordUtil
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
long exportStart = System.currentTimeMillis()
def em = new EM()
def gs = new GS()
em.openEntitySettingsHistoryFromLanding()
String rowText = gs.getFirstVisibleHistoryRowText()
gs.clickHistoryExport()
gs.verifyNewCsvDownloadPresent(exportStart)
String path = gs.getLatestHistoryExportCsvPath(exportStart)
gs.verifyGlobalExportedHistoryCsvHasColumnHeaders(path)
gs.verifyExportedCsvContainsUiRowSnippet(path, rowText)
if (path && gs.respondsTo('deleteLastHistoryExportFile', path)) {
	gs.deleteLastHistoryExportFile(path)
}
WebUI.takeScreenshot()
WebUI.closeBrowser()
