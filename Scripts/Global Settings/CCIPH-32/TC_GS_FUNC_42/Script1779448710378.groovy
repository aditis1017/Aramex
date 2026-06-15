import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS


import customsmanagement.MicrosoftLoginKeywords as MSLogin

	def globalSettings = new GS()
	def login = new MSLogin()

	login.loginToCustomsManagementUsingProfile()
	globalSettings.navigateToGlobalSettings()
	globalSettings.clickDeactivateFirstRule()
	globalSettings.verifyDeactivateConfirmationVisible()
	globalSettings.cancelDeactivateRule()
	globalSettings.openTab(tab2)
	globalSettings.clickDeactivateFirstRule()
	globalSettings.verifyDeactivateConfirmationVisible()
	globalSettings.cancelDeactivateRule()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
