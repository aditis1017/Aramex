// CSV: Entity Management-AR-TestCases.csv | CCIPH-65 | TC_EN_REVENUE_067 | AC1
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEmRevenueServiceChargeEditModeForChargeType(entity, charge_Type ?: 'DTA')
WebUI.takeScreenshot()
WebUI.closeBrowser()