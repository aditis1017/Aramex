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
//cs.selectFiltersById(
//	//'ccfe-customs-charge-code': 'ECD',
//	'ccfe-customs-charge-status': 'Due',
//	'ccfe-customs-business-type': 'B2C',
//	'ccfe-customs-category': 'High Value Shipment',
//	'ccfe-customs-search-criteria': 'Tracking Number'
//)
//cs.enterTrackingNumber('999888777')
cs.enterTrackingNumber("9229011980_B1")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
//charge.selectChargeTypeTab('DTA')
charge.verifyStatusBadgeValue("Category", "LV")
charge.verifyDetailFieldValue("Customs Type", "DDP")
charge.verifyDetailFieldValue("Business Type", "B2C")
String cifValue =
charge.getChargeDetailFieldCIFValue("CIF Value")

String estimate =
charge.getEstimateValueByDescription("Import VAT")

charge.verifyEstimateValueByDescription("Admin Fee")
charge.verifyEstimateValueByDescription("Customs Duty Inbound")
charge.verifyEstimateValueByDescription("Disbursement Fee")
charge.verifyEstimateValueByDescription("Other Charges")
charge.verifyEstimateValueByDescription("Other Customs Charges")
charge.verifyEstimateValueByDescription("Transit Charges")

em.navigateToStageEntitySettings()
em.verifyEntitySettingsPageLoaded()
em.selectEntity('DHA')
em.clickEntitySelectionContinue()

charge.verifyRate1PercentageAndRate2FixedAgainstEstimate(
    "Import VAT",
    "ALL",
    "B2C",
    "LV",
    cifValue,
    estimate
)

cs.navigateToStageCustomsSearchScreen()
cs.enterTrackingNumber("9229011980_B1")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()

// SAR Estimate from Charge Detail — same value validated in verifyRate1PercentageAndRate2FixedAgainstEstimate
charge.verifyDtaUsdConversionForEligibleCostAndOutstanding(
	"Import VAT",
	estimate,
	"USD",
	"0.2666"
)

WebUI.takeScreenshot()
WebUI.closeBrowser()
