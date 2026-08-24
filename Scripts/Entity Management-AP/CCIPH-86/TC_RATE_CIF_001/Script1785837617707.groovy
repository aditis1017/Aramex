// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_001 — CIF in Percentage Of
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetAddNew()
em.fillEntityRateSheetNewRowWithRateDetails(serviceCode, rs_Shipment_Type, 'Percentage', rateVal, rs_Percent_Of ?: 'CIF Value')
em.clickEntityRateSheetSaveRow()
em.clickEntityRateSheetRate1Link(rateVal)
em.verifyEntityRateSheetCifInPercentOfDropdown()
em.clickEntityRateSheetCreationBack()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.takeScreenshot(); WebUI.closeBrowser()
