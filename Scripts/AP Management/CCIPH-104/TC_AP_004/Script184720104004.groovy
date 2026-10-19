import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira: CCIPH-618 | Story: CCIPH-104
try {
	def login = new MSLogin()
	def ap_management = new APManagement()
	login.loginToCustomsManagementUsingAutomationProfile()
	ap_management.navigateToApManagement()
	ap_management.verifyApSearchPageLoaded()
	ap_management.openApAccountDetailFromSearchResults('Account Name', 'Contains', 'Automation-', 'RUH')
	ap_management.verifyApAccountDetailEditableFields()
	WebUI.takeScreenshot()
} catch (Exception e) {
	WebUI.takeScreenshot()
	KeywordUtil.markFailedAndStop(e.getMessage())
} finally {
	WebUI.closeBrowser()
}
