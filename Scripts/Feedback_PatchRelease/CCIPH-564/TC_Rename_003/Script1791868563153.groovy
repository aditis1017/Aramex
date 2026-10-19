import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import entitymanagement.BusinessActions as EM
// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_009

def login = new MSLogin()
def ar_management = new AR()
def em = new EM()
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
//login.loginToStageCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'


// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.

em.navigateToEntitySettings()
em.verifyEntitySettingsPageLoaded()
em.selectEntity('RUH')
em.clickEntitySelectionContinue()
//ar_management.navigateToStageArManagement()
em.verifyColumnHeader(
	'estimate-ratesheet-heading',
	'Shipment Business Type'
)
//em.openEntitySettingsRevenueArRateSheetSettings(entityLabel, chargeType)
em.clickRevenueArSection()
em.selectChargeType('DTC')
em.verifyColumnHeader(
    'estimate-ratesheet-heading',
    'Shipment Business Type'
)
WebUI.takeScreenshot()
WebUI.closeBrowser()