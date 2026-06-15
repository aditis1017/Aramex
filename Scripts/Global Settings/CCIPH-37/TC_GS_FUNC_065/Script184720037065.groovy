import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// History-Custom-Invalid — start = current date; end cannot be 'Current Date +1'

def login = new MSLogin()
def globalSettings = new GS()

def dateFmt = DateTimeFormatter.ofPattern('dd-MM-yyyy')
def todayStr = LocalDate.now().format(dateFmt)


KeywordUtil.logInfo("History-Custom-Invalid: start=today (${todayStr}), verify end cannot be tomorrow")

login.loginToCustomsManagementUsingProfile()
globalSettings.openGlobalSettingsHistoryFromLanding()

// Part 1 — Start Date = current date; End Date must not accept current date + 1
globalSettings.applyHistoryCustomStartDateAsCurrentDate()
globalSettings.verifyHistoryEndDateMaximumIsCurrentDate()
globalSettings.verifyHistoryEndDateTomorrowDisabledInCalendar()


WebUI.takeScreenshot()
WebUI.closeBrowser()
