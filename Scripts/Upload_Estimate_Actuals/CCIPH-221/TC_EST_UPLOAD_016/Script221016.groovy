import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
//
//// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_016
//
def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'
String uploadPath = 'Upload Files/AWB-3-Actuals-Upload-RUH.xlsx'
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
cs.clickBackButton()
cs.enterTrackingNumber('B2BSHIPDATA200')
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber('B2BSHIPDATA200')
//cs.verifyDateFieldIsCurrentDate('Created Date')
//cs.verifyDateFieldIsCurrentDate('09/08/2026')
cs.verifyDateField("Created Date", "09/08/2026")
cs.verifyDateField("Pickup Date", "09/10/2027")

//uea.verifyEstimateAuditLogReminder()
WebUI.takeScreenshot()
WebUI.closeBrowser()














//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import Upload_Estimate_Actuals.BusinessActions as UEA
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//import customsearch_360viewscreen.BusinessActions as CS
//
//// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_002
//// Upload file: Upload Files/Actuals-Upload-RUH.xlsx
//
//def login = new MSLogin()
//def uea = new UEA()
//def cs = new CS()
//
//String uploadPath = 'Upload Files/Actuals-Upload-RUH.xlsx'
//if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
//	uploadPath = valid_Upload_Path as String
//}
//
//login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToStageCustomsSearchScreen()
//uea.clickUploadCharge()
//uea.selectUploadChargesDropdownValue('entity', 'RUH')
//uea.uploadEstimateExcelFile(uploadPath)
//uea.clickSubmitUpload()
//uea.verifyEstimateUploadSuccess()



