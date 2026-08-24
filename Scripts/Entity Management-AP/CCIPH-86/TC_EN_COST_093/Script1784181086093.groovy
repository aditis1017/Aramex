// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_093
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
em.setEntityRateSheetStartEndDates(start_Date, end_Date)
em.clickEntityRateSheetSaveRow()
KeywordUtil.logInfo('Start>End date case executed â€” assert UI/error per environment')
WebUI.takeScreenshot(); WebUI.closeBrowser()
