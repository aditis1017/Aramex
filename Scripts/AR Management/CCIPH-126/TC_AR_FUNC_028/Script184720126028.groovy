import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-126 | TC_AR_FUNC_028 | Disabled read-only inputs on DTC / DTS / DT3

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailForChargeCodeBilling(search_Criteria, search_By, search_Value, entity)
ar_management.verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabs(
	readonly_Label1,
	readonly_Label2,
	readonly_Label3,
	readonly_Label4
)

WebUI.takeScreenshot()
WebUI.closeBrowser()
