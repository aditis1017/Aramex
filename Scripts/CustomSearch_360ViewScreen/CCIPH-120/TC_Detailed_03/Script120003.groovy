import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-120 | TC_Detailed_03

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyEcdTrackingOperationStripUI(tracking_Numbers)
cs.verifyCardLabelAndValue("Tracking Number", "DP13000")
cs.verifyCardLabelAndValue("BILLING ENTITY", "RUH")
cs.verifyCardLabelAndValue("BILLING COUNTRY", "SA")
cs.verifyCardLabelAndValue("MAWB (CONSOLE)", "999888777")

WebUI.takeScreenshot()
WebUI.closeBrowser()
