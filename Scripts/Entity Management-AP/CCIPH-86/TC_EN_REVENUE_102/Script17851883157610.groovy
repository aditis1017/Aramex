// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_102
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetAddNew()
em.fillEntityRateSheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rateVal)
em.clickEntityRateSheetSaveRow()
em.verifyEntityRateSheetRateValuePresent(rateVal)
WebUI.mouseOver(findTestObject('Object Repository/AR Management/CCIPH-86/lnk_ratesheetRate1Value'))
WebUI.takeScreenshot()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.closeBrowser()
