// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_007 — CIF persisted after save
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.saveEntityRateSheetWithPercentOfAndVerifyPersisted(serviceCode, rs_Shipment_Type, rateVal, 'CIF Value')
KeywordUtil.logInfo('CIF selection persisted in UI after save; DB parity is manual/API follow-up')
WebUI.takeScreenshot(); WebUI.closeBrowser()

