// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_108
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
def em = new EM(); def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.openEntitySettingsCostApRateSheetSettings(alternate_Entity, charge_Type)
em.verifyEntityRateSheetSettingsTableColumnsFromData(rs_Columns)
WebUI.takeScreenshot(); WebUI.closeBrowser()
