import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management-Data.csv | CCIPH-200 | TC_AP_FUNC_041 | AC1 — create AP Account with mandatory data

def login = new MSLogin()
def ap_management = new APManagement()
def account_Number = String.valueOf(1000000000000L + (System.currentTimeMillis() % 9000000000000L))
def account_Name = 'Automation_Test_' + String.valueOf(System.currentTimeMillis() % 100000)

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.openCreateApAccountPage()
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
