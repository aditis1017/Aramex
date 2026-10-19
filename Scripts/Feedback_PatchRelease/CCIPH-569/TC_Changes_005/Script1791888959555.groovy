import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import ChargeCreation.BusinessActions as CC
import b2bshipment.BusinessActions as B2B
// CSV: AR-Management.csv | CCIPH-116 | TC_AR_FUNC_009

def login = new MSLogin()
def ar_management = new AR()
def cs = new CS()
def charge = new CC()
def b2b = new B2B()
'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()


//cs.navigateToCustomsSearchScreen()
cs.navigateToStageCustomsSearchScreen()
cs.verifySearchInputForCriteria('Tracking Number', '10010000088')
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
//charge.selectChargeTypeTab('DTA')
//cs.verifyFieldValue("AR Account Name", accountName)
//cs.verifyFieldValue("Shipment Business Type", accountBusinessType)
String consigneeName = cs.readAndWriteConsigneeName()

List<String> nameWords = cs.getConsigneeNameWords()
//cs.clickBackButton()

//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
b2b.verifyB2BKeywordsManagerPageLoaded()

List<String> matchedWords = cs.searchStoredWordsInB2BKeywords(
	nameWords
)

WebUI.takeScreenshot()
WebUI.closeBrowser()