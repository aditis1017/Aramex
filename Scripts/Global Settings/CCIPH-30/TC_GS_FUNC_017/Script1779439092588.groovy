import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS

def tcId = 'TC_GS_FUNC_017'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.verifyRuleOrderTargetValue(targetValue)
WebUI.takeScreenshot()
WebUI.closeBrowser()
