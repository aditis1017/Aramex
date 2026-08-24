// CSV: Entity Management-AR-TestCases.csv | CCIPH-63 | TC_EN_REVENUE_040
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEmRevenueServiceChargeColumns(entity, charge_Type ?: 'DTC', expected_Columns ?: '')
WebUI.takeScreenshot()
WebUI.closeBrowser()
