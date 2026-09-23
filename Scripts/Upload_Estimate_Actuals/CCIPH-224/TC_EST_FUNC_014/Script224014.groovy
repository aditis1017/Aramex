// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_014 | Validate special characters are not present in column headers
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
String path = uea.downloadEstimatesTemplate()
uea.verifyEstimateTemplateHeadersNoSpecialChars(path)
uea.deleteLastEstimateTemplateFile(path)
WebUI.takeScreenshot()
WebUI.closeBrowser()
