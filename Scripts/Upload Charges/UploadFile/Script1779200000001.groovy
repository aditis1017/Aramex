import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customs.UploadChargesActions as Upload
import internal.GlobalVariable

def upload = new Upload()

upload.navigateToUploadCharges()
upload.uploadChargesFile()
upload.clickSubmitUpload()

WebUI.closeBrowser()
