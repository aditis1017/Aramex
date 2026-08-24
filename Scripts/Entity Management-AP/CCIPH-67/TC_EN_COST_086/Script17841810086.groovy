import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_086'
// CSV: Entity Management-AP.xlsx | CCIPH-67 | TC_EN_COST_086

def em = new EM()
def login = new MSLogin()

'Precondition: authenticated session (login handled separately)'
//login.loginToCustomsManagementUsingProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
//em.navigateToEntitySettings()
em.navigateToStageEntitySettings()
em.openEntitySettingsCostApServiceChargeSettings(entity, charge_Type)
em.verifyEntityServiceChargeViewOnlyAccess(entity, charge_Type)

WebUI.takeScreenshot()
WebUI.closeBrowser()
