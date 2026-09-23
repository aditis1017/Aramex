import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_006 | AC2 error message

def login = new MSLogin()
def uea = new UEA()
String invalidPath = 'Data Files/fixtures/CCIPH-221/invalid_format.pdf'

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
uea.uploadEstimateNonXlsxFile(invalidPath)
uea.clickSubmitUpload()
uea.verifyInvalidFileFormatError('Invalid file format')
WebUI.takeScreenshot()
WebUI.closeBrowser()

//covered in TC_EST_UPLOAD_005
