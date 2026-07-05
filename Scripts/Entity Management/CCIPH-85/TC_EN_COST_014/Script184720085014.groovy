import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_014'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_014

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
//em.verifyInvoicingFieldForChargeType(entity, charge_Type, field_Label, false)

em.openEntitySettingsCostAp(entity)
em.clickEntitySelectionContinue()
em.verifyFieldIsReadOnly("country-ro")
em.verifyFieldIsReadOnly("cost-entity")


WebUI.takeScreenshot()
//WebUI.closeBrowser()


