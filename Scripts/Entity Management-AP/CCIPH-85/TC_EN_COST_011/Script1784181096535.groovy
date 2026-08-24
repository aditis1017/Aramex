import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_011'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_011

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
em.openEntitySettingsCostAp(entity)
em.clickEntitySelectionContinue()
em.clickProfileTab('EC3')
em.verifyInputField("country", "SA")
em.verifyInputField("cost-entity", "JED")
em.updateInputField("sap-company-code", "UAE")
em.clickSaveButton()
em.saveButtonPopUp()
em.verifyInputField("sap-company-code", "UAE")

WebUI.takeScreenshot()
WebUI.closeBrowser()


