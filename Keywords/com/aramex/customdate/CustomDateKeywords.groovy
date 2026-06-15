package com.aramex.customdate

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import org.openqa.selenium.Keys

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

public class CustomDateKeywords {

	@Keyword
	def openGlobalSettings(String url) {
		WebUI.openBrowser('')
		WebUI.navigateToUrl(url)
	}

	@Keyword
	def loginWithMicrosoft(String email, String otpCode) {
		WebUI.click(findTestObject('CustomDate/Page_Custom Charges App/button_Sign in with Microsoft'))
		WebUI.setText(findTestObject('CustomDate/Page_Sign in to your account/input_UserIDaramex.com'), email)
		WebUI.sendKeys(findTestObject('CustomDate/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))
		WebUI.setText(findTestObject('CustomDate/Page_Sign in to your account/input_Enter code'), otpCode)
		WebUI.sendKeys(findTestObject('CustomDate/Page_Sign in to your account/input_Enter code'), Keys.chord(Keys.ENTER))
	}

	@Keyword
	def navigateToGlobalSettingsHistory() {
		WebUI.click(findTestObject('CustomDate/Page_Custom Charges App/span_Global Settings'))
		WebUI.click(findTestObject('CustomDate/Page_Custom Charges App/button_HISTORY'))
	}

	@Keyword
	def setHistoryDateRange(String startDate, String endDate) {
		WebUI.click(findTestObject('CustomDate/Page_Global Settings History  Customs Management/button_Custom Range'))
		WebUI.setText(findTestObject('CustomDate/Page_Global Settings History  Customs Management/input_Start Date'), startDate)
		WebUI.setText(findTestObject('CustomDate/Page_Global Settings History  Customs Management/input_End Date'), endDate)
	}
}
