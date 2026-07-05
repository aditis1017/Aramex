import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_026'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_026

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
em.openEntitySettingsCostApChargeType(entity, primary_Charge_Type)
em.verifyChargeTypeIndependence(entity, primary_Charge_Type, other_Charge_Type, 'Entry Limit')
WebUI.takeScreenshot()
WebUI.closeBrowser()

