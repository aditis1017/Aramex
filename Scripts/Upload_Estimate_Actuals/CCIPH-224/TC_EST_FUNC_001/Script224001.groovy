// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_001 | Validate successful download of Estimates Excel Template from Upload Charges screen
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_001

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'DP13000'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData("Estimates")
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.exportEstimatesTemplateAndVerifySuccess()
WebUI.takeScreenshot()
WebUI.closeBrowser()
