import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_Rules_001'
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
globalSettings.openTab('DDU')
globalSettings.clickAddNewRule()
globalSettings.verifyFieldDropdownValuePresent('Shipment Business Type')
globalSettings.verifyFieldDropdownValuePresent('Account Business Type')
globalSettings.clickModalCancel()

globalSettings.openTab('DDP')
globalSettings.clickAddNewRule()
globalSettings.verifyFieldDropdownValuePresent('Shipment Business Type')
globalSettings.verifyFieldDropdownValuePresent('Account Business Type')

WebUI.takeScreenshot()
WebUI.closeBrowser()
