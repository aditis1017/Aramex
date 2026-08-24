import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_ECS_MAP_011'
// CSV: Entity Management-AP.xlsx | CCIPH-67 | TC_ECS_MAP_011

def em = new EM()
def login = new MSLogin()

'Precondition: authenticated session (login handled separately)'
//login.loginToCustomsManagementUsingProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
//em.navigateToEntitySettings()
em.navigateToStageEntitySettings()
em.selectEntity(entity)
em.clickEntitySelectionContinue()
em.clickCostApSection()
em.selectChargeType(charge_Type)
em.verifyEcsEc3ServiceCodeMappingSectionVisible()
em.selectEntity(alternate_Entity)
em.verifyEcsEc3ServiceCodeMappingSectionVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
