// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_011
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
gs.clickHistoryExport()
String exportPath = gs.verifyLatestGlobalSettingsHistoryExportDownloaded(exportStart, null)
KeywordUtil.logInfo("Entity History export: ${exportPath}")
if (exportPath && gs.respondsTo('deleteLastHistoryExportFile', exportPath)) {
	gs.deleteLastHistoryExportFile(exportPath)
}
WebUI.takeScreenshot()
WebUI.closeBrowser()
