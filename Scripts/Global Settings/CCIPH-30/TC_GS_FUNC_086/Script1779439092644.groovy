import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin


def tcId = 'TC_GS_FUNC_086'
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.openRuleActionAndVerifyCategoryTargetValues()
globalSettings.clickModalCancel()
globalSettings.verifyModalNotVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
