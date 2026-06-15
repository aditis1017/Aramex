import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_GS_FUNC_019'
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
globalSettings.clickAddNewRule()
globalSettings.clickModalCancel()
globalSettings.verifyModalNotVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
