// CSV: Entity Management-AR-TestCases.csv | CCIPH-37 | TC_REVENUE_HS_001
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.clickEntitySettingsHistoryButton()
new globalsettings.BusinessActions().verifyGlobalSettingsHistoryPageLoaded()
WebUI.takeScreenshot()
WebUI.closeBrowser()
