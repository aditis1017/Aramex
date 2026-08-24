// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_112
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
def em = new EM(); def login = new MSLogin()
KeywordUtil.logInfo('Customs Manager permission â€” run with Customs Manager profile if available')
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.openEntitySettingsCostApRateSheetSettings(entity, charge_Type)
em.clickEntityRateSheetAddNew()
WebUI.takeScreenshot(); WebUI.closeBrowser()
