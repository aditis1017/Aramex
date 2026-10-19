import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import ChargeCreation.BusinessActions as CC
// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_009

def login = new MSLogin()
def ar_management = new AR()
def cs = new CS()
def charge = new CC()

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
//ar_management.navigateToArManagement()
ar_management.navigateToStageArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.performArSearch('Account Number', 'Contains', 'ASL001', 'RUH')
ar_management.clickFirstArAccountNumberLink()
ar_management.verifyArDtaPageLoaded()

String accountName = ar_management.readAndWriteFieldValue('Account Name')
String accountBusinessType = ar_management.readAndWriteFieldValue('Account Business Type')

//cs.navigateToCustomsSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.verifySearchInputForCriteria('Tracking Number', '9229011982_B23')
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
charge.selectChargeTypeTab('DTA')
cs.verifyFieldValue("AR Account Name", accountName)
cs.verifyFieldValue("Shipment Business Type", accountBusinessType)

WebUI.takeScreenshot()
WebUI.closeBrowser()