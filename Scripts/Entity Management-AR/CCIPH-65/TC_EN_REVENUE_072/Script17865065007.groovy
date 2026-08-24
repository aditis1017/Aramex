// CSV: Entity Management-AR-TestCases.csv | CCIPH-65 | TC_EN_REVENUE_072 | AC2
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.editEmRevenueServiceChargeAttributesSave(entity, charge_Type ?: 'DT3', yes_Label ?: 'Yes', no_Label ?: 'No', billable_Label ?: 'Billable', non_Billable_Label ?: 'Non-Billable')
WebUI.takeScreenshot()
WebUI.closeBrowser()