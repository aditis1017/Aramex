// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_009
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import com.kms.katalon.core.util.KeywordUtil
import globalsettings.BusinessActions as GS
import java.time.LocalDate
import java.time.format.DateTimeFormatter
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def gs = new GS()
def todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern('dd-MM-yyyy'))
KeywordUtil.logInfo("Entity Revenue History custom invalid: start=today (${todayStr})")
em.openEntitySettingsHistoryFromLanding()
gs.applyHistoryCustomStartDateAsCurrentDate()
gs.verifyHistoryEndDateMaximumIsCurrentDate()
gs.verifyHistoryEndDateTomorrowDisabledInCalendar()
WebUI.takeScreenshot()
WebUI.closeBrowser()
