// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_014 | AC4
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyRevenueArChargeTypeIndependence(entity, charge_Type ?: 'DTC', other_Charge_Type ?: 'DTA')
WebUI.takeScreenshot()
WebUI.closeBrowser()