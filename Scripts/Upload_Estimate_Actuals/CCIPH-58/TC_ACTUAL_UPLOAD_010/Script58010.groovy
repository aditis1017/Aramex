import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-58 | TC_ACTUAL_UPLOAD_010 | AC5

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
String invalidAwb = (binding.hasVariable('invalid_AWB') && invalid_AWB) ? invalid_AWB as String : 'INVALID_AWB_XYZ_999999'
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
String path = uea.createMixedValidInvalidActualsExcel(awb, invalidAwb)
uea.uploadActualFileAndSubmit(path)
uea.verifyPartialProcessingWithFailedRecords()
uea.verifyFailedRecordsReportColumns()
WebUI.takeScreenshot()
WebUI.closeBrowser()
