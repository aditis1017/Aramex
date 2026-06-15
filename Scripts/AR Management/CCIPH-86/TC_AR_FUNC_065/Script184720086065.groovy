import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-86 | TC_AR_FUNC_065 — max 3 rate slots; + hidden after Rate 3

def login = new MSLogin()
def ap = new AP()
	
'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.navigateToArManagement()
ap.verifyArSearchPageLoaded()
ap.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
ap.clickArRatesheetAddNew()
ap.verifyArRatesheetRateSlotVisible(1)
ap.clickArRatesheetAddRatePlus()
ap.verifyArRatesheetRateSlotVisible(2)
ap.clickArRatesheetAddRatePlus()
ap.verifyArRatesheetRateSlotVisible(3)
ap.verifyArRatesheetAddRatePlusNotVisible()

WebUI.takeScreenshot()
WebUI.closeBrowser()
