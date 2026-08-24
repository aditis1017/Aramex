// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_006 — Cost of Goods path unchanged
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.saveEntityRateSheetWithPercentOfAndVerifyPersisted(serviceCode, rs_Shipment_Type, rateVal, 'Cost of Goods')
KeywordUtil.logInfo('Cost of Goods Rate Type path saved — shipment calc regression is out of browser scope')
WebUI.takeScreenshot(); WebUI.closeBrowser()
