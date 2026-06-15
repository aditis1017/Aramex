package customs

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import internal.GlobalVariable

/**
 * Upload Charges page — https://dev-cc.aramex.com/customs/upload-charges
 */
class UploadChargesActions {

	private static final String OR_PREFIX = 'Upload Charges/'
	private static final int DEFAULT_TIMEOUT = 30

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static void failStop(String message) {
		try {
			WebUI.takeScreenshot()
		} catch (Exception ignored) {
		}
		KeywordUtil.markFailedAndStop(message)
	}

	/** Strips accidental quotes from profile/script values (e.g. "C:\path\file.xlsx"). */
	private static String normalizeFilePath(String raw) {
		if (!raw) {
			return ''
		}
		String path = raw.trim()
		while ((path.startsWith('"') && path.endsWith('"')) || (path.startsWith("'") && path.endsWith("'"))) {
			path = path.substring(1, path.length() - 1).trim()
		}
		return path
	}

	private static void verifyFileExists(String path) {
		File file = new File(path)
		if (!file.isFile()) {
			failStop("File not found on disk: ${path}")
		}
	}

	@Keyword
	def navigateToUploadCharges(String url = null) {
		try {
			String targetUrl = (url ?: GlobalVariable.UPLOAD_CHARGES_URL ?: '').trim()
			if (!targetUrl) {
				failStop('UPLOAD_CHARGES_URL is not set in profile')
			}
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			WebUI.navigateToUrl(targetUrl)
			// Wait for file input (reliable); title xpath is optional fallback in OR
			WebUI.waitForElementPresent(to('inp_customChargesFile'), DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToUploadCharges failed: ${e.message}")
		}
	}

	/**
	 * Uploads .xls / .xlsx via #custom-charges-file (no Robot / clipboard).
	 */
	@Keyword
	def uploadChargesFile(String filePath = null) {
		try {
			String path = normalizeFilePath(filePath ?: GlobalVariable.UPLOAD_FILE_PATH ?: '')
			if (!path) {
				failStop('uploadChargesFile failed: file path is empty')
			}
			verifyFileExists(path)

			TestObject fileInput = to('inp_customChargesFile')
			WebUI.waitForElementPresent(fileInput, DEFAULT_TIMEOUT)

			try {
				WebUI.uploadFile(fileInput, path)
			} catch (Exception uploadEx) {
				KeywordUtil.logInfo("WebUI.uploadFile failed, retrying with sendKeys: ${uploadEx.message}")
				uploadFileViaSendKeys(fileInput, path)
			}

			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadChargesFile failed: ${e.message}")
		}
	}

	private static void uploadFileViaSendKeys(TestObject fileInput, String path) {
		def element = WebUiCommonHelper.findWebElement(fileInput, DEFAULT_TIMEOUT)
		WebUI.executeScript(
			'arguments[0].style.display = "block"; arguments[0].style.visibility = "visible"; arguments[0].style.opacity = "1";',
			[element]
		)
		element.sendKeys(path)
	}

	@Keyword
	def clickSubmitUpload() {
		try {
			TestObject submit = to('btn_submitUpload')
			WebUI.waitForElementClickable(submit, DEFAULT_TIMEOUT)
			WebUI.click(submit)
			WebUI.delay(2)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("clickSubmitUpload failed: ${e.message}")
		}
	}

	@Keyword
	def uploadActualsTemplate(String filePath = null) {
		navigateToUploadCharges()
		uploadChargesFile(filePath)
		clickSubmitUpload()
	}
}
