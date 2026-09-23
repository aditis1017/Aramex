import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_012

def login = new MSLogin()
def uea = new UEA()
String awb = (binding.hasVariable('invalid_AWB') && invalid_AWB) ? invalid_AWB as String : 'INVALID_AWB_XYZ_999999'

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
String path = uea.createInvalidAwbEstimateExcel(awb)
uea.uploadEstimateFileAndSubmit(path)
uea.verifyEstimateUploadValidationErrorVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
