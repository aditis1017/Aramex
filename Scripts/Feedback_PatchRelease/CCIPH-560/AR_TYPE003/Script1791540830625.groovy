import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_009

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
//login.loginToStageCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
//ar_management.navigateToStageArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.performArSearch('Account Number', 'Contains', '75800042', 'RUH')
ar_management.clickFirstArAccountNumberLink()
ar_management.verifyArDtaPageLoaded()
//ar_management.verifyToggleEnabled('Special Service Charge Settings')
ar_management.verifyFieldNotPresentInSection(
	'Account Settings',
	'Customs Type'
	)
//ar_management.editArServiceChargeActiveToggleAndCancel()
ar_management.navigateToArDtcTabAndVerifyPageLoaded()
ar_management.verifyFieldNotPresentInSection(
'Account Settings',
'Customs Type'
)
ar_management.navigateToArDtsTabAndVerifyPageLoaded()
ar_management.verifyFieldNotPresentInSection(
	'Account Settings',
	'Customs Type'
	)
ar_management.navigateToArDt3TabAndVerifyPageLoaded()
ar_management.verifyFieldNotPresentInSection(
	'Account Settings',
	'Customs Type'
	)
WebUI.takeScreenshot()
WebUI.closeBrowser()
