// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_013 | AC1
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEntitySelectedAndRevenueChargeTypesEnabled(entity)
em.navigateToEntitySettings()
em.verifyEntitySelectedAndRevenueChargeTypesEnabled(alternate_Entity ?: 'RUH')
WebUI.takeScreenshot()
WebUI.closeBrowser()
