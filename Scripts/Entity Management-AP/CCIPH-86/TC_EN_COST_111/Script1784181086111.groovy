// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_111
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
def em = new EM(); def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.verifyEntityRateSheetIndependentAcrossChargeTypes(entity, charge_Type ?: 'ECD', alternate_Charge_Type ?: 'ECS')
WebUI.takeScreenshot(); WebUI.closeBrowser()
