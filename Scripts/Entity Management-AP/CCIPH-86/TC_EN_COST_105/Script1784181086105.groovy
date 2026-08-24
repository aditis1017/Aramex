// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_105
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
em.fillEntityRateSheetNewRowWithRateDetails(serviceCode, rs_Shipment_Type, 'Percentage', rateVal, rs_Percent_Of)
em.clickEntityRateSheetSaveRow()
em.clickEntityRateSheetRate1Link(rateVal)
em.verifyEntityRateSheetValueFieldVisibilityForRateType('Percentage')
em.clickEntityRateSheetCreationBack()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.takeScreenshot(); WebUI.closeBrowser()
