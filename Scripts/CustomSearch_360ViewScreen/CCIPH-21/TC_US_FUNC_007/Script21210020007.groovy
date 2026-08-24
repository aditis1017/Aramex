import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_007 | Invalid date range (End Date < Start Date)

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectDateTypeField(date_Type)
//cs.setCustomDate('Start Date', start_Date)
//cs.setCustomDate('End Date', end_Date)
cs.clickSearch()
cs.verifyDateTypeRequiredErrorVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
