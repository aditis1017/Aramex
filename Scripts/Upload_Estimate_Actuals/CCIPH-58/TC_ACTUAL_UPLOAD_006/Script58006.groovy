import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CSV: Upload Estimate_Actuals.csv | CCIPH-58 | TC_ACTUAL_UPLOAD_006 | AC2

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
//uea.openUploadActualsScreen()
//uea.selectUploadChargesDropdownValue('entity', entity)
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
String path = uea.createCorruptedActualsXlsx()
uea.uploadActualFileAndSubmit(path)
uea.verifyEstimateUploadValidationErrorVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
