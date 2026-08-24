import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Stage login only — https://stage-cc.aramex.com/login (Automation_User / Automation_Password from profile)

def login = new MSLogin()
login.loginToStageCustomsManagementUsingAutomationProfile()

WebUI.takeScreenshot()
WebUI.closeBrowser()
