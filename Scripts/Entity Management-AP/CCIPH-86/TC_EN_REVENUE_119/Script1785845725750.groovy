// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_119
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
KeywordUtil.logInfo('Finance Officer view-only — switch profile to FO before run')
em.navigateToEntitySettings()
em.verifyEntityRevenueRateSheetViewOnlyAccess(entity, charge_Type ?: 'DTC')
WebUI.takeScreenshot(); WebUI.closeBrowser()
