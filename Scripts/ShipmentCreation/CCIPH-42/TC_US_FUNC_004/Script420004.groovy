//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import ShipmentCreation.BusinessActions as SC
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//
//def tcId = 'TC_US_FUNC_004'
//// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | TC_US_FUNC_004
//
//def sc = new SC()
//def login = new MSLogin()
//'This is for Login Profile'
//login.loginToStageCustomsManagementUsingAutomationProfile()
//sc.createDduGlobalRule(rule_Scenario)
//sc.validateCustomScreenForRule(expected_Behavior)
//WebUI.takeScreenshot()
//WebUI.closeBrowser()

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import globalsettings.GsUserCciph30Data as TestData
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import ShipmentCreation.BusinessActions as SC

def tcId = 'TC_GS_FUNC_018'
def globalSettings = new GS()
def login = new MSLogin()
def sc= new SC()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
globalSettings.navigateToGlobalSettings()
globalSettings.openTab(tab)
globalSettings.clickAddNewRule()
globalSettings.selectConditionFieldOperatorAndValueByLabel(conditionField, conditionOperator, valuesComparator)
globalSettings.clickModalSave()
sc.validateCustomScreenForRule(expected_Behavior)
WebUI.takeScreenshot()
WebUI.closeBrowser()

