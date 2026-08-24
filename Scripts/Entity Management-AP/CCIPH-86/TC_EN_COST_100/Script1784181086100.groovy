// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_100
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
em.fillEntityRateSheetNewRowFromData(serviceCode ?: 'SVC001', rs_Shipment_Type ?: 'DDU', rs_Rate_Type ?: 'Fixed', rateVal)
em.clickEntityRateSheetSaveRow()
em.clickEntityRateSheetRate1Link(rateVal)
em.verifyEntityRateSheetCreationColumns()
em.clickEntityRateSheetCreationBack()
em.deleteEntityRateSheetRowByDescription(rateVal)
WebUI.takeScreenshot(); WebUI.closeBrowser()
