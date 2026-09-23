// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_023 | Validate no hidden sheets/tabs exist in exported template
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
uea.verifyEstimateTemplateNoHiddenSheets(path)
uea.deleteLastEstimateTemplateFile(path)
WebUI.takeScreenshot()
WebUI.closeBrowser()
