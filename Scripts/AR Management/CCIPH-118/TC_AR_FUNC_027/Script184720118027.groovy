import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-118 | TC_AR_FUNC_027 | AC2 editable fields
def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailFromSearchResults(search_Criteria, search_By, search_Value, entity)
ap.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ap.navigateToArDtcTabAndVerifyPageLoaded()
ap.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ap.navigateToArDtsTabAndVerifyPageLoaded()
ap.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)
ap.navigateToArDt3TabAndVerifyPageLoaded()
ap.verifyArAccountDetailEditableFields(dta_Field1, dta_Field2, dta_Field3)

WebUI.takeScreenshot()
WebUI.closeBrowser()
