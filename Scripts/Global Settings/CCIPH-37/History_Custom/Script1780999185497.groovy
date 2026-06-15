import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

// History_Custom — end = today, start = today - 90 days (end date selected first).

def login = new MSLogin()
def globalSettings = new GS()

def daysBack = 90

KeywordUtil.logInfo("History_Custom: applying relative custom range (${daysBack} days back to today)")

login.loginToCustomsManagementUsingProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()
globalSettings.applyCustomHistoryDateRangeRelative(daysBack)
globalSettings.verifyHistoryCustomDateRangeRelativeApplied(daysBack)
globalSettings.verifyHistoryTableShowsRecordsForCustomRange()

WebUI.takeScreenshot()
WebUI.closeBrowser()
