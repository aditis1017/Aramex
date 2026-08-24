import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Entity Management AP- TestCases.xlsx | CCIPH-69 | TC_EN_COST_075 | Cancel edit row without saving

def em = new EM()
def login = new MSLogin()

'Precondition: authenticated session (login handled separately)'
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.editEmServiceChargeActiveCancelForAllCostChargeTypes(entity)

WebUI.takeScreenshot()
WebUI.closeBrowser()
