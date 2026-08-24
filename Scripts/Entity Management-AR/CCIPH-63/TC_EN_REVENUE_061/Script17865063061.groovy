// CSV: Entity Management-AR-TestCases.csv | CCIPH-63 | TC_EN_REVENUE_061
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEmRevenueServiceChargeUiDbReminder(entity, charge_Type ?: 'DTC')
WebUI.takeScreenshot()
WebUI.closeBrowser()
