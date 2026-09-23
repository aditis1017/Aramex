// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_016 | Validate multiple downloads of template work successfully
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Estimates')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
int count = (binding.hasVariable('multiple_Download_Count') && multiple_Download_Count) ? (multiple_Download_Count as int) : 3
uea.verifyMultipleEstimateTemplateDownloads(count)
WebUI.takeScreenshot()
WebUI.closeBrowser()
