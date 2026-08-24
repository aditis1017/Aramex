// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_011 | AC2
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueAr(entity)
em.verifyAllRevenueArInvoicingFieldsFunctionalForChargeType(charge_Type ?: 'DTS')
WebUI.takeScreenshot()
WebUI.closeBrowser()