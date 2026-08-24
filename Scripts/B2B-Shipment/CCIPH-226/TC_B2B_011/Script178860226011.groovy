import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_011 | Reject non-.xlsx
// Upload file: Katalon project Upload Files/B2B_Invalid.csv (select All Files equivalent via accept filter clear)

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Invalid.csv'
if (binding.hasVariable('invalid_Upload_Path') && invalid_Upload_Path) {
	uploadPath = invalid_Upload_Path as String
}

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload invalid .csv from Katalon project Upload Files folder (All Files)'
b2b.uploadB2BNonXlsxFile(uploadPath)
b2b.verifyInvalidB2BFileFormatError()
b2b.verifySubmitDisabledWhenNoFile()
WebUI.takeScreenshot()
WebUI.closeBrowser()
