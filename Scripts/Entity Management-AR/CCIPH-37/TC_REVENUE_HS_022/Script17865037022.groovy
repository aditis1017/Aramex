// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_022
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import com.kms.katalon.core.util.KeywordUtil
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def gs = new GS()
em.openEntitySettingsHistoryFromLanding()
gs.clickHistoryDateFilter(history_Filter ?: 'ALL')
int rows = gs.countHistoryTableDataRows()
KeywordUtil.logInfo("Entity Revenue History visible row count: ${rows}")
gs.verifyHistoryTableStillDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()
