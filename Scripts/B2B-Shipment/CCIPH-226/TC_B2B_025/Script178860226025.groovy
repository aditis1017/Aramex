import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_025 | Exact keyword match for consignee classification
// Upload file: Katalon project Upload Files/B2B_TC25.xlsx

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_TC25.xlsx'
if (binding.hasVariable('exact_Upload_Path') && exact_Upload_Path) {
	uploadPath = exact_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload B2B_TC25.xlsx, submit, and verify Keyword/Match Type rows + footer count'
b2b.uploadB2BExcelFileAndSubmit(uploadPath)
b2b.verifyUploadedB2BKeywordsFromFile(uploadPath)
WebUI.takeScreenshot()
WebUI.closeBrowser()
