import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_GS_FUNC_014'
def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
//def ruleValue ='93224'
def globalSettings = new GS()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()

//login.loginToStageCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
//globalSettings.navigateToStageGlobalSettings()

globalSettings.createDduRuleWithCostOfGoods(ruleValue)
//globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, 'Equals', destination)
globalSettings.verifySavedRuleWithCondition(conditionField, ruleValue, conditionOperator, destination)
WebUI.takeScreenshot()
WebUI.closeBrowser()
