import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_017 | DB verification out of browser scope

def login = new MSLogin()
def uea = new UEA()
String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
String path = uea.createValidEstimateExcel(awb)
uea.uploadEstimateFileAndSubmit(path)
uea.verifyEstimateUploadSuccess()
uea.verifyEstimateDatabaseStorageReminder()
WebUI.takeScreenshot()
WebUI.closeBrowser()
