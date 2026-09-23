import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_001 | AC1 screen access
// Precondition: Login as System Admin (stripped from steps — executed via profile)

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.openUploadEstimatesScreen()
uea.clickUploadFile()
//uea.verifyUploadEstimatesScreenControlsVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
