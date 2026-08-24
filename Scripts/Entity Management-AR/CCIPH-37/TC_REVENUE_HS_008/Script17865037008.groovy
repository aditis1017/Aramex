// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_008
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import com.kms.katalon.core.util.KeywordUtil
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def gs = new GS()
int days = ((days_Back ?: '90').toString().trim() ?: '90') as int
KeywordUtil.logInfo("Entity Revenue History custom range: ${days} days back")
em.openEntitySettingsHistoryFromLanding()
gs.applyCustomHistoryDateRangeRelative(days)
gs.verifyHistoryCustomDateRangeRelativeApplied(days)
gs.verifyHistoryTableShowsRecordsForCustomRange()
WebUI.takeScreenshot()
WebUI.closeBrowser()
