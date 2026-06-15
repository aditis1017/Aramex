import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import apmanagement.ApCciph102Data as ApData
import apmanagement.BusinessActions as APManagement
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AP-Management.csv | CCIPH-102 | TC_AP_FUNC_011

def tcId = 'TC_AP_FUNC_011'
def login = new MSLogin()
def ap_management = new APManagement()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap_management.navigateToApManagement()
ap_management.verifyApSearchPageLoaded()
ap_management.performSearch(search_Criteria, search_By, search_Value, entity)
ap_management.verifyNoRecordsFoundMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
