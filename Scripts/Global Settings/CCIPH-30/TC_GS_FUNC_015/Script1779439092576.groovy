import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_GS_FUNC_015'
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
WebUI.delay(2)
globalSettings.clickAddNewRule()
globalSettings.verifyAddRuleModalElementsPresent()
WebUI.takeScreenshot()
WebUI.closeBrowser()
