import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import b2bshipment.BusinessActions as B2B
// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_005
// Upload file: Upload Files/Actuals-Upload-RUH-Invalid.csv (select All Files)

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
def b2b = new B2B()

String uploadPath = 'Upload Files/Actuals-Upload-RUH-Invalid.csv'
if (binding.hasVariable('invalid_Upload_Path') && invalid_Upload_Path) {
	uploadPath = invalid_Upload_Path as String
}

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectUploadChargesDropdownValue('entity', 'RUH')
//uea.uploadInvalidFileType(uploadPath)
b2b.uploadB2BNonXlsxFile(uploadPath)
//uea.clickSubmitButton()
//uea.verifyInvalidFileFormat()
uea.verifyErrorMessage('Invalid file format')
WebUI.takeScreenshot()
WebUI.closeBrowser()
