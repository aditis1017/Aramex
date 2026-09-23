// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_003 | Validate downloaded template contains mandatory columns
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

//String uploadPath = 'Upload Files/Actuals-Upload-RUH.xlsx'
//if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
//	uploadPath = valid_Upload_Path as String
//}
//String path = downloadEstimatesTemplate()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Estimates')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
String path = uea.downloadEstimatesTemplate()
uea.verifyEstimateTemplateMandatoryColumns(path)
uea.deleteLastEstimateTemplateFile(path)
WebUI.takeScreenshot()
WebUI.closeBrowser()
