import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS

def tcId = 'TC_GS_FUNC_083'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.verifyAddRuleModalElementsPresent()
WebUI.takeScreenshot()
WebUI.closeBrowser()
