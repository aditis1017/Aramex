//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import Upload_Estimate_Actuals.BusinessActions as UEA
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//import customsearch_360viewscreen.BusinessActions as CS
//
//// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_010
//
//def login = new MSLogin()
//def uea = new UEA()
//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'
//
//login.loginToStageCustomsManagementUsingAutomationProfile()
//uea.navigateToUploadCharges()
//uea.openUploadEstimatesScreen()
//String path = uea.createExtraColumnEstimateExcel(awb)
//uea.uploadEstimateFileAndSubmit(path)
//uea.verifyEstimateUploadSuccess()
//WebUI.takeScreenshot()
//WebUI.closeBrowser()
//
//
//


import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_010

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath = 'Upload Files/Extra-Column-Actuals-Upload-RUH-Data.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath = valid_Upload_Path as String
}

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.uploadEstimateExcelFile(uploadPath)
uea.clickSubmitUpload()
uea.verifyEstimateUploadSuccess()
WebUI.takeScreenshot()
WebUI.closeBrowser()



