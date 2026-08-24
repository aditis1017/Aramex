// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_021
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def gs = new GS()
def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
gs.clickHistoryDateFilter(history_Filter ?: 'ALL')
long t0 = System.currentTimeMillis()
gs.verifyHistoryPaginationNextChangesFooterOrFirstRow()
long elapsed = System.currentTimeMillis() - t0
KeywordUtil.logInfo("Entity History page navigation elapsed ms: ${elapsed}")
WebUI.takeScreenshot()
WebUI.closeBrowser()
