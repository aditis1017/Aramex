import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_010 | Match Type Contains
// Upload file: Katalon project Upload Files/B2B_Contains.xlsx (all rows use Match Type = Contains)

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Contains.xlsx'
if (binding.hasVariable('contains_Upload_Path') && contains_Upload_Path) {
	uploadPath = contains_Upload_Path as String
}

'This is for Login Profile'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
//b2b.navigateToB2BKeywordsManager()
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload B2B_Contains.xlsx, submit, and verify Keyword/Match Type Contains rows + footer count'
b2b.uploadB2BExcelFileAndSubmit(uploadPath)
b2b.verifyUploadedB2BKeywordsFromFile(uploadPath)
WebUI.takeScreenshot()
WebUI.closeBrowser()
