import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData

def tcId = 'TC_GS_FUNC_019'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.clickAddNewRule()
globalSettings.clickModalCancel()
globalSettings.verifyModalNotVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
