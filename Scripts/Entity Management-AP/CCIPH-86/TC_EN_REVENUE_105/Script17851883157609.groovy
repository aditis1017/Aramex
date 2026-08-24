// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_105 | AC2
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetAddNew()
em.fillEntityRateSheetNewRowFromData(serviceCode ?: 'SVC001', rs_Shipment_Type ?: 'DDU', rs_Rate_Type ?: 'Fixed', rateVal)
em.clickEntityRateSheetSaveRow()
em.clickEntityRateSheetRate1Link(rateVal)
em.verifyEntityRateSheetCreationColumns()
em.clickEntityRateSheetCreationBack()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.takeScreenshot(); WebUI.closeBrowser()
