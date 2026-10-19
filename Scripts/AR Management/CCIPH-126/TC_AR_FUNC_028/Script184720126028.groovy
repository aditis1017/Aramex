import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-126 | TC_AR_FUNC_028 | Disabled read-only inputs on DTC / DTS / DT3

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailForChargeCodeBilling('Account Number', 'Contains', '8004', entity)
ar_management.verifyArAccountDetailDisabledReadOnlyInputsOnChargeCodeTabsV2(
	readonly_Label1,
	readonly_Label2,
	readonly_Label3,
	readonly_Label4
)

ar_management.verifyFieldsReadOnly(
    [
        'Account Name',
        'Account Number',
        'Country',
        'Entity',
        'Account Type',
        'Account Business Type',
        'Business Segment'
    ]
)


WebUI.takeScreenshot()
WebUI.closeBrowser()
