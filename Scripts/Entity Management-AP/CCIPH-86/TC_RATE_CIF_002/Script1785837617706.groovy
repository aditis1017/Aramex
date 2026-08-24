// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_002 — SH013 CosG-only CIF calc (UI config; shipment assert out of browser)
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.saveEntityRateSheetWithPercentOfAndVerifyPersisted(serviceCode, rs_Shipment_Type, rateVal, 'CIF Value')
KeywordUtil.logInfo('SH013: when only Cost of Goods present, CIF Value = Cost of Goods — verify via shipment calc/API outside UI')
WebUI.takeScreenshot(); WebUI.closeBrowser()
