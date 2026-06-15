import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS

def tcId = 'TC_GS_FUNC_091'
def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.addCostOfGoodsFieldAndValue(ruleValue)
globalSettings.selectTargetValueByIndex(1)
globalSettings.saveRuleModal()
globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, conditionOperator, destination)
globalSettings.deactivateSavedRuleWithCondition(conditionField, ruleValue, conditionOperator, destination)
globalSettings.verifyActivateRuleButtonWithCondition(conditionField, ruleValue, conditionOperator, destination)
WebUI.takeScreenshot()
WebUI.closeBrowser()
