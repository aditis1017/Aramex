import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.BrowserKeywords
// as BK
// CSV: Upload Estimate_Actuals.csv | CCIPH-58 | TC_ACTUAL_UPLOAD_006 | AC2

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()
//def brefresh = new BK()

String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'
String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : '3002298711111'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
//uea.openUploadActualsScreen()
//uea.selectUploadChargesDropdownValue('entity', entity)
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', 'RUH')
String path = uea.createValidActualsBOEExcel(awb)
uea.uploadActualFileAndSubmit(path)
uea.verifyActualUploadSuccess()
cs.clickBackButton()
cs.enterTrackingNumber(awb)
cs.clickSearch()
//cs.hardReloadAndClearCache()
cs.validateFirstRow()
cs.clickTrackingNumber(awb)
//cs.verifyInvoicingTableUI('9229011982_B23')
cs.hardReloadAndClearCache()
cs.verifyInvoiceLabelValue("BOE Number", "7733ASAER")
WebUI.takeScreenshot()
WebUI.closeBrowser()
