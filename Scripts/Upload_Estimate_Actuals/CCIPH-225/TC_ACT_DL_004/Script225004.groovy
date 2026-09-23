//// CSV: Upload Estimate_Actuals.csv | CCIPH-225 | TC_ACT_DL_004 | Verify that Operations Staff can access template export functionality
//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import Upload_Estimate_Actuals.BusinessActions as UEA
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//import customsearch_360viewscreen.BusinessActions as CS
//
//def login = new MSLogin()
//def uea = new UEA()
//def cs = new CS()
//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
//
//login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToStageCustomsSearchScreen()
//uea.clickUploadCharge()
//// Precondition: Login as Operations Staff — configure MS_LOGIN_EMAIL in profile if role-specific account available
//uea.verifyExportActualsTemplateButtonVisibleAndEnabled()
//WebUI.takeScreenshot()
//WebUI.closeBrowser()
