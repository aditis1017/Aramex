import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CCIPH-569 | TC_Changes_008_1 — Actuals upload: read AWB / Declaration Number / Saddad from Excel and validate on 360 View

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath = 'Upload Files/Patch-AWB-New-Actuals-Upload-RUH.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath = valid_Upload_Path as String
}
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', entity)
uea.uploadActualExcelFile(uploadPath)

// Option A: one map → assign String variables for assertions
Map<String, String> actualsFromUpload = uea.readActualsUploadKeyFieldsAsStrings(uploadPath)
String awb = actualsFromUpload['AWB']
String declarationNumber = actualsFromUpload['Declaration Number']
String saddad = actualsFromUpload['Saddad']

// Option B (equivalent after read/store): uea.getStoredActualsUploadKeyFields() or uea.readValue('AWB')

KeywordUtil.logInfo(
	"Actuals from upload file — AWB: '${awb}', Declaration Number: '${declarationNumber}', Saddad: '${saddad}'"
)

uea.clickSubmitActualUpload()
uea.verifyActualUploadSuccess()
WebUI.delay(5)
cs.clickBackButton()

cs.enterTrackingNumber(awb)
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber(awb)
cs.verifyDetailPageLoaded()

cs.verifyInvoiceLabelValue('BOE Number', declarationNumber)


cs.verifyInvoiceLabelValue('PR Number', saddad)

WebUI.takeScreenshot()
WebUI.closeBrowser()
