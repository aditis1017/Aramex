// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_116 | AC3
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEntityRevenueRateSheetIndependentAcrossChargeTypes(entity, charge_Type ?: 'DTC', alternate_Charge_Type ?: 'DTA')
WebUI.takeScreenshot(); WebUI.closeBrowser()

