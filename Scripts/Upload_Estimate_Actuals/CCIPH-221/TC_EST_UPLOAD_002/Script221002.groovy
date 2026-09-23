import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_002
// Upload file: Upload Files/Actuals-Upload-RUH.xlsx

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath = 'Upload Files/Actuals-Upload-RUH.xlsx'
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
