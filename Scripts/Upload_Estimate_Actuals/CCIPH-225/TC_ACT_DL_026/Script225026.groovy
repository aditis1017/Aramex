// CSV: Upload Estimate_Actuals.csv | CCIPH-225 | TC_ACT_DL_026 | Verify export button alignment and UI consistency
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
uea.verifyExportActualsButtonUiConsistency()
WebUI.takeScreenshot()
WebUI.closeBrowser()
