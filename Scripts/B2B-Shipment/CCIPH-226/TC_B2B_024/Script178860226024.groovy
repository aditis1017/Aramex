import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_024 | Contains keyword for consignee classification
// Upload file: Katalon project Upload Files/B2B_Consignee.xlsx

def login = new MSLogin()
def b2b = new B2B()

String uploadPath = 'Upload Files/B2B_Consignee.xlsx'
if (binding.hasVariable('consignee_Upload_Path') && consignee_Upload_Path) {
	uploadPath = consignee_Upload_Path as String
}

'This is for Login Profile'
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to B2B Keywords Manager'
b2b.navigateToStageB2BKeywordsManager()
WebUI.takeScreenshot()
'Upload B2B_Consignee.xlsx, submit, and verify Keyword/Match Type rows + footer count'
b2b.uploadB2BExcelFileAndSubmit(uploadPath)
b2b.verifyUploadedB2BKeywordsFromFile(uploadPath)
WebUI.takeScreenshot()
WebUI.closeBrowser()
