// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_017
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def gs = new GS()
def login = new MSLogin()
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
gs.verifyHistoryPaginationNextChangesFooterOrFirstRow()
WebUI.takeScreenshot()
WebUI.closeBrowser()
