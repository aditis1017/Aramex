// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_014
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def gs = new GS()
em.openEntitySettingsHistoryFromLanding()
gs.verifyHistoryPaginationDefaultPageSizeFifty()
WebUI.takeScreenshot()
WebUI.closeBrowser()
