import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-126 | TC_AR_FUNC_028 | Disabled read-only inputs on DTC / DTS / DT3

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailForChargeCodeBilling(search_Criteria, search_By, search_Value, entity)
ap.verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs(
	readonly_Label1,
	readonly_Label2,
	readonly_Label3,
	readonly_Label4,
	readonly_Label5
)

WebUI.takeScreenshot()
WebUI.closeBrowser()
