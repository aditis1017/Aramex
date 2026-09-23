import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Upload Estimate_Actuals.csv | CCIPH-221 | TC_EST_UPLOAD_014

def login = new MSLogin()
def uea = new UEA()
int rows = 200
if (binding.hasVariable('large_Row_Count') && large_Row_Count) {
	rows = Integer.parseInt(large_Row_Count.toString())
}

login.loginToStageCustomsManagementUsingAutomationProfile()
uea.navigateToUploadCharges()
uea.openUploadEstimatesScreen()
String path = uea.createLargeEstimateExcel(rows)
uea.uploadEstimateExcelFile(path)
uea.clickSubmitUpload()
uea.verifyEstimateUploadPerformance(120000L)
WebUI.takeScreenshot()
WebUI.closeBrowser()
