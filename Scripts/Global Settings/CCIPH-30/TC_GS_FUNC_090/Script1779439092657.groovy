import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS

def tcId = 'TC_GS_FUNC_090'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickDeactivateFirstRule()
globalSettings.verifyDeactivateConfirmationVisible()
globalSettings.confirmDeactivateRule()
WebUI.takeScreenshot()
WebUI.closeBrowser()
