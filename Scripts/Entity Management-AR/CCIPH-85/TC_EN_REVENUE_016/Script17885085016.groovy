// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_016 | AC5
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: Finance Officer authenticated session (LOGIN-TEST_SUITE / FO profile) - login not executed in feature script.
def em = new EM()
KeywordUtil.logInfo('Run with Finance Officer profile session')
em.navigateToEntitySettings()
em.verifyRevenueArInvoicingViewOnlyAccess(entity, charge_Type ?: 'DTC')
WebUI.takeScreenshot()
WebUI.closeBrowser()