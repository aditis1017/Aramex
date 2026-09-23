import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_018

def login = new MSLogin()
def uea = new UEA()

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
String path = uea.createCorruptedEstimateXlsx()
uea.uploadEstimateFileAndSubmit(path)
uea.verifyEstimateUploadValidationErrorVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
