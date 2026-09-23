import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import ChargeCreation.BusinessActions as CC
import entitymanagement.BusinessActions as EM
import ap_ar_management.BusinessActions as AR

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_017

def cs = new CS()
def login = new MSLogin()
def charge = new CC()
def em = new EM()
def ar_management = new AR()

long exportStart = System.currentTimeMillis()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectFiltersById(
	//'ccfe-customs-charge-code': 'ECD',
	'ccfe-customs-charge-status': 'Due',
	'ccfe-customs-business-type': 'B2C',
	'ccfe-customs-category': 'High Value Shipment',
	'ccfe-customs-search-criteria': 'Tracking Number'
)
//cs.enterTrackingNumber('999888777')
cs.enterTrackingNumber("877023")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
//charge.selectChargeTypeTab('DTA')
charge.verifyStatusBadgeValue("Category", "HV")
charge.verifyDetailFieldValue("Customs Type", "DDU")
charge.verifyDetailFieldValue("Business Type", "B2C")
//cs.verifyDescription("Admin Fee")
charge.selectChargeTypeTab('DTC')
charge.verifyDescriptionContractedRateAndOutstandingSame("Other Charges")

String arAccountNumber =
charge.getChargeDetailFieldValue("AR Account #")

ar_management.navigateToStageArManagement()
ar_management.performArSearch(
	'Account Number',
	'Contains',
	arAccountNumber,
	'RUH'
)
//ar_management.verifySearchResultsTableVisible()
//ar_management.clickFirstArAccountNumberLink()
ar_management.clickAccountResult(
	"Account Number",
	arAccountNumber
)
ar_management.selectArChargeTypeTab("DTC")
ar_management.toggleArSpecialRateSheetSettings()
ar_management.verifyRatesheetAndNavigateIfEmpty()
em.verifyEntitySettingsPageLoaded()
em.selectEntity('RUH')
em.clickEntitySelectionContinue()
em.selectEntitySettingsTab("Revenue-AR")

//charge.verifyRateType(
//	"Other Charges",
//	"DDU",
//	"B2C",
//	"HV",
//	"Fixed"
//)
charge.verifyEntityRatesheetRowValues(
	"Other Charges",
	"DDU",
	"B2C",
	"HV"
)

String rate1 = charge.getRate1ValueForEntity(
	"Other Charges",
	"DDU",
	"B2C",
	"HV",
	"Fixed"
)

// Re-open Charge Detail after Entity Settings, select DTC, then assert Contracted Rate == Outstanding
charge.selectChargeTypeAndVerifyContractedRateAndOutstanding(
    "DTC",
    "Other Charges",
    "877023"
)

WebUI.takeScreenshot()
WebUI.closeBrowser()