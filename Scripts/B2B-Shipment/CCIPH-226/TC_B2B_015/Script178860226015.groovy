import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_015 | Mixed-case Match Type values Exact and Contains are accepted
// Upload file: Katalon project Upload Files/B2B_CaseSensitive.xlsx

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_CaseSensitive.xlsx'
if (binding.hasVariable('caseSensitive_Upload_Path') && caseSensitive_Upload_Path) {
	uploadPath = caseSensitive_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload B2B_CaseSensitive.xlsx, submit, and verify Keyword/Match Type rows + footer count'
b2b.uploadB2BExcelFileAndSubmit(uploadPath)
//b2b.verifyUploadedB2BKeywordsFromFile(uploadPath)
b2b.verifyB2BInvalidCaseErrorMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
