// CSV: Upload Estimate_Actuals.csv | CCIPH-224 | TC_EST_FUNC_009 | Validate EXPORT ESTIMATES TEMPLATE button visibility for Finance Officer role
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
// Precondition: Login as Finance Officer — set MS_LOGIN_EMAIL in profile for that role when available
uea.verifyExportEstimatesTemplateButtonVisibleAndEnabled()
WebUI.takeScreenshot()
WebUI.closeBrowser()
