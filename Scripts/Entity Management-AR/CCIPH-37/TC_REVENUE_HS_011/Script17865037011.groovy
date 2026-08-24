// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_011
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import com.kms.katalon.core.util.KeywordUtil
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
long exportStart = System.currentTimeMillis()
def em = new EM()
def gs = new GS()
em.openEntitySettingsHistoryFromLanding()
gs.clickHistoryExport()
String exportPath = gs.verifyLatestGlobalSettingsHistoryExportDownloaded(exportStart, null)
KeywordUtil.logInfo("Entity Revenue History export: ${exportPath}")
if (exportPath && gs.respondsTo('deleteLastHistoryExportFile', exportPath)) {
	gs.deleteLastHistoryExportFile(exportPath)
}
WebUI.takeScreenshot()
WebUI.closeBrowser()
