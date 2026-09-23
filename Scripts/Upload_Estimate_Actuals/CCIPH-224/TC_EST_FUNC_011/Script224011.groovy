// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_011 | Validate button hidden/disabled for unauthorized users
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
//uea.selectTypeOfData('Estimates')
//uea.selectUploadChargesDropdownValue('entity', 'RUH')
// Precondition: Login with user having no upload estimate permission (e.g. Viewer) — set MS_LOGIN_EMAIL in profile
uea.verifyExportEstimatesTemplateButtonHiddenOrDisabled()
WebUI.takeScreenshot()
WebUI.closeBrowser()
