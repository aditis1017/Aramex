import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-31 | TC_GS_FUNC_031 | Figma: https://taffy-legal-55683069.figma.site/
// Precondition: Super Admin session on Global Settings URL.

	def newVal = String.valueOf(System.currentTimeMillis() % 100000)
	def globalSettings = new GS()
	def login = new MSLogin()

	// 1) Microsoft login (session + cookies)
	'This is for Login Profile'
    login.loginToCustomsManagementUsingAutomationProfile()
    //login.loginToStageCustomsManagementUsingAutomationProfile()
    'This is used for Navigating to Management'
    globalSettings.navigateToGlobalSettings()
    //globalSettings.navigateToStageGlobalSettings()
	WebUI.delay(2)
	globalSettings.openTab(tab)
	globalSettings.clickEditFirstRuleContainingCostOfGoods()
	globalSettings.verifyRuleModalVisible()
	globalSettings.verifyConditionValueFieldNotEmpty()
	globalSettings.enterConditionValue(newVal)
	globalSettings.saveRuleModal()
	//globalSettings.verifyRuleUpdateSuccessTitle("Rule Updated Successfully")
	globalSettings.dismissRuleUpdateSuccessModalIfPresent()
	globalSettings.verifySavedRuleWithCondition(conditionField, newVal, conditionOperator, destination)
	KeywordUtil.logInfo('AC4 (non-retroactive application): UI cannot assert that historical shipment/charge rows are unchanged; confirm with backend or domain reporting if required.')
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
