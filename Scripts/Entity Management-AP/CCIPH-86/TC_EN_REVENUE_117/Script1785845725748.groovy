// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_117
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
KeywordUtil.logInfo('Customs Manager permission — run with Customs Manager profile if available')
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetAddNew()
WebUI.takeScreenshot(); WebUI.closeBrowser()
