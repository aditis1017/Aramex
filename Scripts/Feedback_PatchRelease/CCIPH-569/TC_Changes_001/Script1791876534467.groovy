import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToCustomSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.selectDropdownValueList("ccfe-customs-business-type", 'B2B')
cs.selectDropdownValueList("ccfe-customs-category", 'Low Value Shipment')
//cs.selectDropdownValueList("category", category)
cs.verifySearchInputForCriteria('Tracking Number', '9229011982_B23')

cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()

//cs.verifySectionAndFieldLabelsPresent('Customs & Goods', 'Insurance Value')
cs.verifySectionAndFieldLabelsPresent('Customs & Goods', 'Insurance Value')

WebUI.takeScreenshot()
WebUI.closeBrowser()