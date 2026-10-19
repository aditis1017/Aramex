import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

// CCIPH-569 | TC_Changes_008_1 — Actuals upload: read AWB / Declaration Number / Saddad from Excel and validate on 360 View

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String uploadPath1 = 'Upload Files/AWB1-New-Actuals-Upload-RUH.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath1 = valid_Upload_Path as String
}
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.clickUploadCharge()
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', entity)
uea.uploadActualExcelFile(uploadPath1)

// Option A: one map → assign String variables for assertions
Map<String, String> actualsFromUpload1 = uea.readActualsUploadKeyFieldsAsStrings(uploadPath1)
String awb1 = actualsFromUpload1['AWB']
String declarationNumber1 = actualsFromUpload1['Declaration Number']
String saddad1 = actualsFromUpload1['Saddad']

// Option B (equivalent after read/store): uea.getStoredActualsUploadKeyFields() or uea.readValue('AWB')

KeywordUtil.logInfo(
	"Actuals from upload file — AWB: '${awb1}', Declaration Number: '${declarationNumber1}', Saddad: '${saddad1}'"
)

uea.clickSubmitActualUpload()
uea.verifyActualUploadSuccess()


WebUI.delay(2)

String uploadPath2 = 'Upload Files/AWB2-New-Actuals-Upload-RUH.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath2 = valid_Upload_Path as String
}
uea.selectTypeOfData('Actuals')
uea.selectUploadChargesDropdownValue('entity', entity)
uea.uploadActualExcelFile(uploadPath2)

Map<String, String> actualsFromUpload2 = uea.readActualsUploadKeyFieldsAsStrings(uploadPath2)
String awb2 = actualsFromUpload2['AWB']
//String declarationNumber2 = actualsFromUpload2['Declaration Number']
//String saddad2 = actualsFromUpload1['Saddad']

KeywordUtil.logInfo(
	"Actuals from upload file — AWB: '${awb2}', Declaration Number: '${declarationNumber1}', Saddad: '${saddad1}'"
)

uea.clickSubmitActualUpload()
uea.verifyActualUploadSuccess()

cs.clickBackButton()

cs.enterTrackingNumber(awb1)
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber(awb1)
cs.verifyDetailPageLoaded()

cs.verifyInvoiceLabelValue('BOE Number', declarationNumber1)


cs.verifyInvoiceLabelValue('PR Number', saddad1)
cs.navigateToStageCustomsSearchScreen()


cs.enterTrackingNumber(awb2)
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber(awb2)
cs.verifyDetailPageLoaded()

cs.verifyInvoiceLabelValue('BOE Number', declarationNumber1)


cs.verifyInvoiceLabelValue('PR Number', saddad1)

WebUI.takeScreenshot()
WebUI.closeBrowser()
