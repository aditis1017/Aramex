import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-120 | TC_Detailed_05

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyEcdAccountInformationUI(tracking_Numbers)
cs.verifyFieldValue("Account Type", "—")
cs.verifyFieldValue("AP Account #", "1100053259")
cs.verifyFieldValue("AP Account Name", "KSA Customs Authorities")
cs.verifyFieldValue("Customs Type", "DDP")
cs.verifyFieldValue("Shipping Charges", "0.00 SAR")
cs.verifyFieldValue("Goods Value", "750.00 USD")
cs.verifyFieldValue("CIF Value", "2812.50 SAR")
cs.verifyFieldValue("Number of Pieces", "1")
cs.verifyFieldValue("Source", "Services")
cs.verifyFieldValue("Product Group", "EXP")
cs.verifyFieldValue("Business Type", "B2B")

WebUI.takeScreenshot()
WebUI.closeBrowser()
