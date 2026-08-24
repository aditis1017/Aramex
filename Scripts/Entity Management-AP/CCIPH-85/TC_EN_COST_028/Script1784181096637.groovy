import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_028'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_028

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
// Precondition: active profile uses Finance Officer credentials
em.verifyEntityInvoicingViewOnlyAccess(entity, charge_Type ?: 'ECD')
WebUI.takeScreenshot()
WebUI.closeBrowser()

