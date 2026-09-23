import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-120 | TC_Detailed_02

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyEcdChargeTabSections(tracking_Numbers)
cs.verifySection("ACCOUNT INFORMATION")
cs.verifySection("ADDITIONAL DETAILS (ORIGIN, DESTINATION & 3RD PARTY)")
WebUI.takeScreenshot()
WebUI.closeBrowser()
