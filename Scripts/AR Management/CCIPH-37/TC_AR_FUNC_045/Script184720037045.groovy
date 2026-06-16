import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import apmanagement.BusinessActions as AP
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: AR-Management.csv | CCIPH-37 | TC_AR_FUNC_045 | AC2 — Custom range start > end, no data

def login = new MSLogin()
def ap = new AP()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Management'
ap.openArManagementHistoryFromLanding()
ap.applyArCustomHistoryDateRangeFromData(history_Start_Date, history_End_Date)
ap.verifyArHistoryShowsNoDataRowsOrZeroCount()

WebUI.takeScreenshot()
WebUI.closeBrowser()
