import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-118 | TC_AR_FUNC_027 | AC2 editable fields

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ar_management.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ar_management.navigateToArDtcTabAndVerifyPageLoaded()
ar_management.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ar_management.navigateToArDtsTabAndVerifyPageLoaded()
ar_management.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ar_management.navigateToArDt3TabAndVerifyPageLoaded()
ar_management.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)

WebUI.takeScreenshot()
WebUI.closeBrowser()
