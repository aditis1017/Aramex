import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// Jira TC: CCIPH-653 | Story: CCIPH-652 | TC_ID: TC_APM_FUNC_001
// cci-katalon-from-jira | cci-automation-from-story | Data: AP-Management-Data

def login = new MSLogin()
def ap_management = new APManagement()
def account_Number = String.valueOf(1000000000000L + (System.currentTimeMillis() % 9000000000000L))
if (account_Number.length() > 15) {
	account_Number = account_Number.substring(0, 15)
}
def account_Name = 'Automation_CCIPH652_' + String.valueOf(System.currentTimeMillis() % 100000)

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.verifyCreateApAccountTypeReadOnlyVendor()
ap_management.createApAccountWithMandatoryFields(
	account_Number,
	account_Name,
	account_Type ?: 'Vendor',
	country,
	create_Entity,
	status ?: 'Active',
	rate_Currency,
	billing_Currency,
	'Other'
)
ap_management.performSearch(search_Criteria ?: 'Account Name', search_By ?: 'Contains', account_Name, entity)
ap_management.verifySearchResultsTableVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
