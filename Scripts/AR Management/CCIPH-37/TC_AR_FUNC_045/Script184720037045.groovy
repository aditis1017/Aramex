import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_045 | AC2 — Custom range start > end, no data

def login = new MSLogin()
def ar_management = new AR()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'This is used for Navigating to Management'
ar_management.openArManagementHistoryFromLanding()
ar_management.applyArCustomHistoryDateRangeFromData(history_Start_Date, history_End_Date)
ar_management.verifyArHistoryShowsNoDataRowsOrZeroCount()

WebUI.takeScreenshot()
WebUI.closeBrowser()


//need to discuss