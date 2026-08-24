import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling
// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_097
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
def em = new EM(); def login = new MSLogin()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.openEntitySettingsCostApRateSheetSettings(entity, charge_Type)
em.clickEntityRateSheetAddNew()
em.fillEntityRateSheetNewRowFromData(serviceCode, rs_Shipment_Type, rs_Rate_Type, rateVal)
em.clickEntityRateSheetSaveRow()
em.verifyEntityRateSheetRateValuePresent(rateVal)
WebUI.mouseOver(findTestObject('Object Repository/AR Management/CCIPH-86/lnk_ratesheetRate1Value'), FailureHandling.OPTIONAL)
WebUI.takeScreenshot()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.closeBrowser()
