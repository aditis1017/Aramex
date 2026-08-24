// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_RATE_CIF_004 — update Cost of Goods to CIF
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import ap_ar_management.BusinessActions as AR
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def ar = new AR()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetAddNew()
em.fillEntityRateSheetNewRowWithRateDetails(serviceCode, rs_Shipment_Type, 'Percentage', rateVal, 'Cost of Goods')
em.clickEntityRateSheetSaveRow()
em.verifyEntityRateSheetRateValuePresent(rateVal)
ar.clickArRatesheetEditRowByRateValue(rateVal)
ar.updateArRatesheetPercentOf('CIF Value', rateVal)
ar.clickSaveEditRatesheet()
em.clickEntityRateSheetRate1Link(rateVal)
em.verifyEntityRateSheetCifInPercentOfDropdown()
em.clickEntityRateSheetCreationBack()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.takeScreenshot(); WebUI.closeBrowser()

