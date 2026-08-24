// CSV: Entity Management AP- TestCases.xlsx | CCIPH-86 | TC_EN_COST_114
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
def em = new EM(); def login = new MSLogin()
KeywordUtil.logInfo('Finance Officer view-only â€” switch profile to FO before run')
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.verifyEntityRateSheetViewOnlyAccess(entity, charge_Type)
WebUI.takeScreenshot(); WebUI.closeBrowser()
