//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import Upload_Estimate_Actuals.BusinessActions as UEA
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//
//// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_009
//
//def login = new MSLogin()
//def uea = new UEA()
//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'
//
//login.loginToStageCustomsManagementUsingAutomationProfile()
//uea.navigateToUploadCharges()
//uea.openUploadEstimatesScreen()
//String path = uea.createMissingColumnEstimateExcel(awb)
//uea.uploadEstimateFileAndSubmit(path)
//uea.verifyEstimateUploadValidationErrorVisible()
//WebUI.takeScreenshot()
//WebUI.closeBrowser()


import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_009
// Upload file: Upload Files/Actuals-Upload-RUH.xlsx

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath = 'Upload Files/AWB-Missing-Actuals-Upload-Template-RUH.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath = valid_Upload_Path as String
}

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.uploadEstimateExcelFile(uploadPath)
//uea.clickSubmitUpload()
uea.clickSubmitButton()
uea.verifyErrorMessage('Your file is missing required column(s): AWB.')
WebUI.takeScreenshot()
WebUI.closeBrowser()