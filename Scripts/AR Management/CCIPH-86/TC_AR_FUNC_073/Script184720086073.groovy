import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_072 | AC4 — Edit newly created RateSheet

def login = new MSLogin()
def ap = new AP()
def rateValue = String.valueOf(System.currentTimeMillis() % 1000)
def description = 'Automation_Test_Description' + String.valueOf(System.currentTimeMillis() % 100000)
def serviceDescription = 'Automation_Test_Service' + String.valueOf(System.currentTimeMillis() % 100000)
def updatedRateValue = String.valueOf(System.currentTimeMillis() % 1000 + 1 )
	
'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ap.clickArRatesheetAddNew()
ap.fillArRatesheetNewRow(description, serviceDescription, rs_Shipment_Type, rs_Rate_Type, rateValue)
//ap.clickArRatesheetRate1ValueLinkForRow(description)
//ap.verifyArRatesheetCreationPageVisible()
ap.clickArRatesheetCreationBack()
//ap.verifyArAccountReceivableDetailAfterRatesheetBack()
ap.verifyArRatesheetSettingsPanelVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
