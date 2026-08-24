// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_008
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def gs = new GS()
def login = new MSLogin()
int days = ((days_Back ?: '90').toString().trim() ?: '90') as int
KeywordUtil.logInfo("Entity History custom range: ${days} days back")
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
gs.applyCustomHistoryDateRangeRelative(days)
gs.verifyHistoryCustomDateRangeRelativeApplied(days)
gs.verifyHistoryTableShowsRecordsForCustomRange()
WebUI.takeScreenshot()
WebUI.closeBrowser()
