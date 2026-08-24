// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_005 — CIF across Revenue charge types
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.verifyEntityRevenueRateSheetCifAcrossChargeTypes(entity, ['DTC', 'DTA', 'DTS', 'DT3'])
WebUI.takeScreenshot(); WebUI.closeBrowser()
