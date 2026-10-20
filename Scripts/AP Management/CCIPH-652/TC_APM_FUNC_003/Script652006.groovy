import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-658 | Story: CCIPH-652 | TC_ID: TC_APM_FUNC_003
// cci-katalon-from-jira | cci-automation-from-story | Data: AP-Management-Data

def login = new MSLogin()
def ap_management = new APManagement()
def account_Number = String.valueOf(2000000000000L + (System.currentTimeMillis() % 8000000000000L))
if (account_Number.length() > 15) {
	account_Number = account_Number.substring(0, 15)
}
def account_Name = 'Automation_CCIPH652_Direct_' + String.valueOf(System.currentTimeMillis() % 100000)

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.createApAccountWithMandatoryFields(
	account_Number,
	account_Name,
	account_Type ?: 'Vendor',
	country,
	create_Entity,
	status ?: 'Active',
	rate_Currency,
	billing_Currency,
	'Direct'
)
WebUI.takeScreenshot()
WebUI.closeBrowser()
