// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_001
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.navigateToEntitySettings()
em.clickEntitySettingsHistoryButton()
new globalsettings.BusinessActions().verifyGlobalSettingsHistoryPageLoaded()
WebUI.takeScreenshot()
WebUI.closeBrowser()
