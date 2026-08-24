import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Entity Management AP- TestCases.xlsx | CCIPH-69 | TC_EN_COST_073 | AC1 Edit Service Code

def em = new EM()
def login = new MSLogin()

'Precondition: authenticated session (login handled separately)'
login.loginToStageCustomsManagementUsingAutomationProfile()
em.navigateToStageEntitySettings()
em.verifyEmServiceChargeEditModeForEcd(entity)

WebUI.takeScreenshot()
WebUI.closeBrowser()
