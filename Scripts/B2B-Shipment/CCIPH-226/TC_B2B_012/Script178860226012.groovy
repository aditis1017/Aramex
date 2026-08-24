import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_012 | Max file size boundary
// Upload file: Katalon project Upload Files/B2B_Valid_35MB+.xlsx (>35 MB)

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Valid_35MB+.xlsx'
if (binding.hasVariable('oversized_Upload_Path') && oversized_Upload_Path) {
	uploadPath = oversized_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload oversized .xlsx from Katalon project Upload Files folder'
b2b.uploadB2BExcelFile(uploadPath)
b2b.verifyB2BFileTooLargeError()
b2b.verifySubmitDisabledWhenNoFile()
WebUI.takeScreenshot()
WebUI.closeBrowser()
