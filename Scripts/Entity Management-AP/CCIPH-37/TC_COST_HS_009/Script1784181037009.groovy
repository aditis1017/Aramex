// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_009
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

import java.time.LocalDate
import java.time.format.DateTimeFormatter

def em = new EM()
def gs = new GS()
def login = new MSLogin()
def todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern('dd-MM-yyyy'))
KeywordUtil.logInfo("Entity History custom invalid: start=today (${todayStr})")
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
gs.applyHistoryCustomStartDateAsCurrentDate()
gs.verifyHistoryEndDateMaximumIsCurrentDate()
gs.verifyHistoryEndDateTomorrowDisabledInCalendar()
WebUI.takeScreenshot()
WebUI.closeBrowser()
