// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_002
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
em.clickEntitySettingsHistoryBackAndVerifyLanding()
WebUI.takeScreenshot()
WebUI.closeBrowser()
