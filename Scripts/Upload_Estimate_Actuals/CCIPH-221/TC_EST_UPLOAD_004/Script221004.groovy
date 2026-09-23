import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_004

def login = new MSLogin()
def uea = new UEA()
String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'
String duty = (binding.hasVariable('customs_Duty') && customs_Duty) ? customs_Duty as String : '100'

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
String path = uea.createValidEstimateExcel(awb, duty, '50', '25')
uea.uploadEstimateFileAndSubmit(path)
uea.verifyEstimateUploadSuccess()
uea.verifyShipmentEstimateUpdatedInUiReminder(awb, duty)
WebUI.takeScreenshot()
WebUI.closeBrowser()
