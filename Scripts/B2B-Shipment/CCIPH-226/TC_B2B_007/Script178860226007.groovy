import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_007 | File staging enables SUBMIT
// Upload file: Katalon project Upload Files/B2B_Valid.xlsx (pre-populated valid data)

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Valid.xlsx'
if (binding.hasVariable('valid_Upload_Path') && valid_Upload_Path) {
	uploadPath = valid_Upload_Path as String
}

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload valid .xlsx from Katalon project Upload Files folder'
b2b.uploadB2BExcelFile(uploadPath)
b2b.verifySubmitEnabled()
WebUI.takeScreenshot()
WebUI.closeBrowser()
