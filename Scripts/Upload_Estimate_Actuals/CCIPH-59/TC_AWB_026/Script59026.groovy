import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import b2bshipment.BusinessActions as B2B

// CSV: Upload Estimate_Actuals.csv | CCIPH-59 | TC_AWB_026 | AC1

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
def b2b = new B2B()

String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
String invalidAwb = (binding.hasVariable('invalid_AWB') && invalid_AWB) ? invalid_AWB as String : 'INVALID_AWB_XYZ_999999'
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
String uploadPath = (binding.hasVariable('invalid_Upload_Path') && invalid_Upload_Path) ? invalid_Upload_Path as String : 'Data Files/fixtures/CCIPH-58/invalid_format.txt'
uea.selectUploadChargesDropdownValue('entity', entity)
b2b.uploadB2BNonXlsxFile(uploadPath)
uea.verifyErrorMessage('Invalid file format')
WebUI.takeScreenshot()
WebUI.closeBrowser()
