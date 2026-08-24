// CSV: Entity Management-AR-TestCases.csv | CCIPH-65 | TC_EN_REVENUE_074 | AC3
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.editEmRevenueServiceChargeAttributesCancel(entity, charge_Type ?: 'DTC', yes_Label ?: 'Yes', no_Label ?: 'No', billable_Label ?: 'Billable', non_Billable_Label ?: 'Non-Billable')
WebUI.takeScreenshot()
WebUI.closeBrowser()