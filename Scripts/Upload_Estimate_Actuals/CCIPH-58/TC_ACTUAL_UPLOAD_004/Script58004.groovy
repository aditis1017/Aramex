import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import b2bshipment.BusinessActions as B2B

// CSV: Upload Estimate_Actuals.csv | CCIPH-58 | TC_ACTUAL_UPLOAD_004 | AC2

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
def b2b = new B2B()

String uploadPath = 'Upload Files/Actuals-Upload-RUH-Invalid.csv'
if (binding.hasVariable('invalid_Upload_Path') && invalid_Upload_Path) {
	uploadPath = invalid_Upload_Path as String
}
//String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.openUploadActualsScreen()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
b2b.uploadB2BNonXlsxFile(uploadPath)
uea.verifyErrorMessage('Invalid file format')
WebUI.takeScreenshot()
WebUI.closeBrowser()
