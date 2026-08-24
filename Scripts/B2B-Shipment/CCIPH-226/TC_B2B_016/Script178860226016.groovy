import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_016 | Keyword max length >50 rejected
// Upload file: Katalon project Upload Files/B2B_CharacterExceed.xlsx

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_CharacterExceed.xlsx'
if (binding.hasVariable('characterExceed_Upload_Path') && characterExceed_Upload_Path) {
	uploadPath = characterExceed_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload keyword length exceed .xlsx from Katalon project Upload Files folder'
b2b.uploadB2BExcelFile(uploadPath)
b2b.verifyB2BUploadValidationErrorBanner()
b2b.verifySubmitDisabledWhenNoFile()
WebUI.takeScreenshot()
WebUI.closeBrowser()
