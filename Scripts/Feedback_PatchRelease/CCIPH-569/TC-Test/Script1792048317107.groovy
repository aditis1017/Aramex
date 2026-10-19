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




cs.enterTrackingNumber('9229011980_11')
cs.clickSearch()
cs.validateFirstRow()
cs.clickTrackingNumber('9229011980_11')
cs.verifyDetailPageLoaded()

cs.verifyInvoiceLabelValue('BOE Number', '1001ABC')


cs.verifyInvoiceLabelValue('PR Number', '1002')

//WebUI.takeScreenshot()
//WebUI.closeBrowser()
