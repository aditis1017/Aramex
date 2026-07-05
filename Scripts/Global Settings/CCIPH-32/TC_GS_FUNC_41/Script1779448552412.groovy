import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

	def globalSettings = new GS()
	def login = new MSLogin()

	login.loginToCustomsManagementUsingAutomationProfile()
	globalSettings.navigateToGlobalSettings()
	globalSettings.clickDeactivateFirstRule()
	globalSettings.verifyDeactivateConfirmationVisible()
	globalSettings.confirmDeactivateRule()
	globalSettings.openTab(tab)
	globalSettings.clickDeactivateFirstRule()
	globalSettings.verifyDeactivateConfirmationVisible()
	globalSettings.confirmDeactivateRule()
	WebUI.takeScreenshot()
	WebUI.closeBrowser()
