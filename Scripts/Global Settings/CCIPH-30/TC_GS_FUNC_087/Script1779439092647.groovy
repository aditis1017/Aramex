import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_GS_FUNC_087'
def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.addCostOfGoodsFieldAndValue(ruleValue)
globalSettings.selectTargetValueByIndex(1)
globalSettings.saveRuleModal()
globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, conditionOperator, destination)
WebUI.takeScreenshot()
WebUI.closeBrowser()
