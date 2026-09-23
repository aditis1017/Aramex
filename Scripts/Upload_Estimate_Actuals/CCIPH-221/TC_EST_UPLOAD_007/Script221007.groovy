import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// Submit Button Disabled by default| CCIPH-221 | TC_EST_UPLOAD_007


def login = new MSLogin()
def uea = new UEA()
def cs = new CS()


login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.verifySubmitButtonDisabled()
WebUI.takeScreenshot()
WebUI.closeBrowser()
