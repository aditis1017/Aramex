// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_012 | AC3
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArChargeType(entity, charge_Type ?: 'DTC')
em.saveRevenueArInvoicingConfiguration(sap_Company_Code ?: ('SA' + String.valueOf(System.currentTimeMillis() % 10000)))
WebUI.takeScreenshot()
WebUI.closeBrowser()