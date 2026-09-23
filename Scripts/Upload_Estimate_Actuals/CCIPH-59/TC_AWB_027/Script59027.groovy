//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import Upload_Estimate_Actuals.BusinessActions as UEA
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//import customsearch_360viewscreen.BusinessActions as CS
//
//// CSV: Upload Estimate_Actuals.csv | CCIPH-59 | TC_AWB_027 | AC1
//
//def login = new MSLogin()
//def uea = new UEA()
//def cs = new CS()
//
//String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
//String invalidAwb = (binding.hasVariable('invalid_AWB') && invalid_AWB) ? invalid_AWB as String : 'INVALID_AWB_XYZ_999999'
//String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'
//
//login.loginToStageCustomsManagementUsingAutomationProfile()
//cs.navigateToStageCustomsSearchScreen()
////uea.openActualsUploadForAwbValidation()
//uea.clickUploadCharge()
//uea.selectTypeOfData('Actuals')
//uea.selectUploadChargesDropdownValue('entity', 'RUH')
//String path = uea.createEmptyActualsExcelFile()
//uea.selectEntityAndUploadActuals(path, entity)
//uea.verifyEstimateUploadValidationErrorVisible()
//WebUI.takeScreenshot()
//WebUI.closeBrowser()
