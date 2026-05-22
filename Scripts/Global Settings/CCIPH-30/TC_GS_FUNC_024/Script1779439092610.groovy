import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData

def tcId = 'TC_GS_FUNC_024'
def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
def globalSettings = new GS()

globalSettings.navigateToGlobalSettings()
globalSettings.clickAddNewRule()
globalSettings.addCostOfGoodsFieldAndValue(ruleValue)
globalSettings.saveRuleModal()
globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, conditionOperator, destination)
globalSettings.clickAddNewRule()
globalSettings.addCostOfGoodsFieldAndValue(ruleValue)
globalSettings.saveRuleModal()
globalSettings.verifyDuplicateRuleMessageVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
