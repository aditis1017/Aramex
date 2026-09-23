import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-58 | TC_ACTUAL_UPLOAD_002 | AC1

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath = 'Upload Files/Actuals-Upload-RUH.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath = valid_Upload_Path as String
}
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.uploadActualExcelFile(uploadPath)
uea.clickSubmitActualUpload()
uea.verifyActualUploadSuccess()
WebUI.takeScreenshot()
WebUI.closeBrowser()
