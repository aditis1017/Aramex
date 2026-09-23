import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import ChargeCreation.BusinessActions as CC
import entitymanagement.BusinessActions as EM

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_017

def cs = new CS()
def login = new MSLogin()
def charge = new CC()
def em = new EM()


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
cs.verifyDescription("Admin Fee")
cs.verifyDescriptionEstimateAndEligibleCostSame("Admin Fee")
em.navigateToStageEntitySettings()
em.verifyEntitySettingsPageLoaded()
em.selectEntity('RUH')
em.clickEntitySelectionContinue()

charge.verifyEntityRatesheetRowValues(
    "Admin Fee",
    "DDU",
    "B2C",
    "HV"
)
charge.verifyRateType(
	"Admin Fee",
	"DDU",
	"B2C",
	"HV",
	"Fixed"
)
//charge.verifyRate1ValueForDescription("Admin Fee")
//charge.verifyRate1ValueFor("Admin Fee",
//    "DDU",
//    "B2C",
//    "HV",
//    "Fixed")

String rate1 = charge.getRate1ValueForEntity(
    "Admin Fee",
    "DDU",
    "B2C",
    "HV",
    "Fixed"
)

// Re-open Charge Detail after Entity Settings Rate 1 lookup, then compare Fixed Rate vs Estimate / Eligible Cost
charge.verifyFixedRateEstimateAndEligibleCostSame(
	"Admin Fee",
	rate1,
	"877023"
)

//cs.verifyRate1ValueForServiceCode("Customs Duty Inbound")



WebUI.takeScreenshot()
//WebUI.closeBrowser()
