import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_070 | AC4 — Edit existing RateSheet row

def login = new MSLogin()
def ar_management = new AR()
	
'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ar_management.clickArRatesheetEditFirstRow()
ar_management.updateArRatesheetRate1Value(AR.generateArRatesheetRandomNumber())
ar_management.clickSaveEditRatesheet()
ar_management.verifyArRatesheetSettingsPanelVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
