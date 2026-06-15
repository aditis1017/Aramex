import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData

def tcId = 'TC_GS_FUNC_018'
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.selectConditionFieldOperatorAndValueByLabel(conditionField, conditionOperator, valuesComparator)
globalSettings.clickModalSave()
globalSettings.verifyDuplicateRuleMessageVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
