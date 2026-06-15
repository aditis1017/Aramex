import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_070 | AC4 — Edit existing RateSheet row

def login = new MSLogin()
def ap = new AP()
	
'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ap.clickArRatesheetEditFirstRow()
ap.updateArRatesheetRate1Value(AP.generateArRatesheetRandomNumber())
ap.clickSaveEditRatesheet()
ap.verifyArRatesheetSettingsPanelVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
