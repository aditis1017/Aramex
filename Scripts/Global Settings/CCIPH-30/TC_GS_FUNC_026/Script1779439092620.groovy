import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData

def tcId = 'TC_GS_FUNC_026'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.clickAddNewRule()
globalSettings.clickAddCondition()
WebUI.delay(2)
globalSettings.verifyDeleteIconsOnAddedConditionRows()
WebUI.takeScreenshot()
WebUI.closeBrowser()
