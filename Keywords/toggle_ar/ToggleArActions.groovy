package toggle_ar

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import org.openqa.selenium.Keys

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/**
 * Actions extracted from "Test Cases/Toggle-AR".
 *
 * NOTE:
 * - Keep test data (email/otp/accountName/entity) as parameters.
 * - Test Objects are referenced exactly as in the original test case.
 */
class ToggleArActions {

	@Keyword
	def openLoginPage(String url = 'https://dev-cc.aramex.com/login') {
		WebUI.openBrowser('')
		WebUI.navigateToUrl(url)
	}

	@Keyword
	def loginWithMicrosoft(String email, String otpCode) {
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/button_Sign in with Microsoft'))

		WebUI.setText(findTestObject('Toggle-AR_TEST/Page_Sign in to your account/input_UserIDaramex.com'), email)
		WebUI.sendKeys(findTestObject('Toggle-AR_TEST/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))

		WebUI.setText(findTestObject('Toggle-AR_TEST/Page_Sign in to your account/input_Enter code'), otpCode)
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Sign in to your account/div_col-xs-24 no-padding-left-right button-conta'))

		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Sign in to your account/input_idSIButton9'))
	}

	@Keyword
	def navigateToArManagement() {
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/div_AR Management'))
	}

	@Keyword
	def searchArAccount(String searchCriteriaValue,
			String searchByValue,
			String accountName,
			String entityValue) {

		WebUI.selectOptionByValue(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/select_Search Criteria'), searchCriteriaValue, false)
		WebUI.selectOptionByValue(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/select_Search By'), searchByValue, false)
		WebUI.setText(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/input_Enter Account Name'), accountName)
		WebUI.selectOptionByValue(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/select_Entity'), entityValue, false)
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/button_SEARCH'))
	}

	@Keyword
	def openFirstSearchResult() {
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/td_8003'))
	}

	@Keyword
	def toggleServiceChargeSettings() {
		// keep same sequence as original
		WebUI.rightClick(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/rect_rect'))
		WebUI.waitForElementPresent(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/rect_rect'), 0)
		WebUI.click(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/circle_transition-all duration-300 ease-in-out'))
	}

	@Keyword
	def verifyServiceChargeSettingsPanelAndColumns(String panelTitle = 'Service Charge Settings',
			String serviceCodeHeader = 'Service Code',
			String descriptionHeader = 'Description') {

		WebUI.verifyElementText(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/h3_Service Charge Settings'), panelTitle)
		WebUI.verifyElementText(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/th_Service Code'), serviceCodeHeader)
		WebUI.verifyElementText(findTestObject('Toggle-AR_TEST/Page_Custom Charges App/th_Description'), descriptionHeader)
	}

	@Keyword
	def close() {
		WebUI.takeScreenshot()
		WebUI.closeBrowser()
	}
}
