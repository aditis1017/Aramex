import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// 1) Microsoft login with Automation_User / Automation_Password only (no OTP)
new MSLogin().loginToCustomsManagementUsingAutomationProfile()

// 2) Example: open Global Settings in same browser (navigateToGlobalSettings no longer forces a new browser)
new GS().navigateToGlobalSettings()

WebUI.takeScreenshot()
