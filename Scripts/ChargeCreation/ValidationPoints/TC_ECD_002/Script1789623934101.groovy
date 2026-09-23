import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import entitymanagement.BusinessActions as EM

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_017

def cs = new CS()
def login = new MSLogin()
def em = new EM()

long exportStart = System.currentTimeMillis()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectFiltersById(
//	'ccfe-customs-charge-code': 'ECD',
	'ccfe-customs-charge-status': 'Due'
)
cs.enterTrackingNumber('LO23_38289857362')
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow() 
cs.verifyServiceCodeAndDescription("Customs Duty Inbound", "Customs Duty Inbound")
cs.verifyEstimateAndEligibleCostSame("Customs Duty Inbound")
em.navigateToStageEntitySettings()
em.verifyEntitySettingsPageLoaded()
em.selectEntity('JED')
em.clickEntitySelectionContinue()
cs.verifyRate1ValueForServiceCode("Customs Duty Inbound")

WebUI.takeScreenshot()
WebUI.closeBrowser()
