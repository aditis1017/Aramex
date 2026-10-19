import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import ap_ar_management.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CCIPH-634 | TC_APM_FUNC_004 | Data: AP-Management-Data iteration 41

def login = new MSLogin()
def ap_management = new APManagement()
def account_Number = String.valueOf(1000000000000L + (System.currentTimeMillis() % 9000000000000L))
def account_Name = 'Automation_CCIPH634_' + String.valueOf(System.currentTimeMillis() % 100000)

login.loginToCustomsManagementUsingAutomationProfile()
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
ap_management.verifyCreateApAccountTypeReadOnlyVendor()
ap_management.createApAccountWithMandatoryFields(
	account_Number,
	account_Name,
	account_Type,
	country,
	create_Entity,
	status,
	rate_Currency,
	billing_Currency,
	market_Structure
)
ap_management.performSearch(search_Criteria, search_By, account_Name, entity)
ap_management.verifySearchResultsTableVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
