import com.aramex.customdate.CustomDateKeywords
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import globalsettings.BusinessActions as GS
CustomDateKeywords cd = new CustomDateKeywords()

String url = 'https://dev-cc.aramex.com/customs-management/global-settings'
String email = 'aditi.singh1@tothenew.com'
String otpCode = ''
String startDate = '2026-05-01'
String endDate = '2026-05-26'
def globalSettings = new GS()

cd.openGlobalSettings(url)
cd.loginWithMicrosoft(email, otpCode)
//cd.navigateToGlobalSettingsHistory()

globalSettings.openGlobalSettingsHistoryFromLanding()
//globalSettings.clickHistoryDateFilter('CUSTOM')
cd.setHistoryDateRange(startDate, endDate)
WebUI.delay(2)
// Optional: close browser at end
WebUI.closeBrowser()
