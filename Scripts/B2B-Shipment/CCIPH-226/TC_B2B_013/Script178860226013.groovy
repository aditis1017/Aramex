import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_013 | Duplicate Keyword+Match Type
// Upload file: Katalon project Upload Files/B2B_Duplicate.xlsx

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Duplicate.xlsx'
if (binding.hasVariable('duplicate_Upload_Path') && duplicate_Upload_Path) {
	uploadPath = duplicate_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload duplicate keyword .xlsx from Katalon project Upload Files folder'
b2b.uploadB2BExcelFile(uploadPath)
b2b.verifyB2BDuplicateKeywordError()
b2b.verifySubmitDisabledWhenNoFile()
WebUI.takeScreenshot()
WebUI.closeBrowser()
