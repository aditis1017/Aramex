// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_022 | Validate template compatibility with upload functionality
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
//String manualEntryPath = 'Upload Files/TC13-Estimates-Upload-RUH.xlsx'
//if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
//	manualEntryPath = valid_Upload_Path as String
//}

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Estimates')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.verifyEstimateTemplateCompatibilityWithUpload(awb)
WebUI.takeScreenshot()
WebUI.closeBrowser()
