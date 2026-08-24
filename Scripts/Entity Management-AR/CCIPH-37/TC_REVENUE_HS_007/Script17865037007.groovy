// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_007
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def gs = new GS()
em.openEntitySettingsHistoryFromLanding()
gs.clickHistoryDateFilter(history_Filter)
gs.verifyHistoryTableStillDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()
