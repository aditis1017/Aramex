// CSV: Entity Management-AR-TestCases.csv | CCIPH-85 | TC_EN_REVENUE_015 | AC5
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: Operation Staff authenticated session (LOGIN-TEST_SUITE / OS profile) - login not executed in feature script.
def em = new EM()
KeywordUtil.logInfo('Run with Operation Staff profile session')
em.navigateToEntitySettings()
em.verifyRevenueArInvoicingViewOnlyAccess(entity, charge_Type ?: 'DTC')
WebUI.takeScreenshot()
WebUI.closeBrowser()