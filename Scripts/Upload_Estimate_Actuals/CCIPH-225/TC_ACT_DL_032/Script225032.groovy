// CSV: Upload Estimate_Actuals.csv | CCIPH-225 | TC_ACT_DL_032 | Verify multiple consecutive downloads of template
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
int count = (binding.hasVariable('multiple_Download_Count') && multiple_Download_Count) ? (multiple_Download_Count as int) : 3
uea.verifyMultipleActualsTemplateDownloads(count)
WebUI.takeScreenshot()
WebUI.closeBrowser()
