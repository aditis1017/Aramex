package business

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import java.util.UUID

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import internal.GlobalVariable

class LoginKeywords {

    private static final int DEFAULT_TIMEOUT = 30

    private static void waitVisibleAndClickable(String testObjectId, int timeout) {
        WebUI.waitForElementPresent(findTestObject(testObjectId), timeout)
        WebUI.waitForElementVisible(findTestObject(testObjectId), timeout)
        WebUI.waitForElementClickable(findTestObject(testObjectId), timeout)
    }

    private static void waitPresentAndVisible(String testObjectId, int timeout) {
        WebUI.waitForElementPresent(findTestObject(testObjectId), timeout)
        WebUI.waitForElementVisible(findTestObject(testObjectId), timeout)
    }

    private static void failStopWithScreenshot(String message) {
        try {
            WebUI.takeScreenshot()
        } catch (Exception ignored) {
            // best-effort screenshot only
        }
        KeywordUtil.markFailedAndStop(message)
    }

    @Keyword
    def commonLogin(String username, String password) {
        try {
            WebUI.openBrowser('')
            WebUI.maximizeWindow()
            WebUI.navigateToUrl(GlobalVariable.URL)

            // Open login form
            waitVisibleAndClickable('pages/login/btn_LoginSignup', DEFAULT_TIMEOUT)
            WebUI.click(findTestObject('pages/login/btn_LoginSignup'))

            // Fill credentials
            waitVisibleAndClickable('pages/login/txt_Username', DEFAULT_TIMEOUT)
            WebUI.setText(findTestObject('pages/login/txt_Username'), username)

            waitVisibleAndClickable('pages/login/txt_Password', DEFAULT_TIMEOUT)
            WebUI.setText(findTestObject('pages/login/txt_Password'), password)

            // Submit
            waitVisibleAndClickable('pages/login/btn_Login', DEFAULT_TIMEOUT)
            WebUI.click(findTestObject('pages/login/btn_Login'))
        } catch (Exception e) {
            failStopWithScreenshot("Failed during commonLogin(): ${e.getClass().getSimpleName()} - ${e.getMessage()}")
        }
    }

    @Keyword
    def loginWithValidCredentials() {
        try {
            commonLogin(GlobalVariable.USERNAME, GlobalVariable.PASSWORD)

            // Validate successful login
            waitPresentAndVisible('pages/login/successful_login', DEFAULT_TIMEOUT)
        } catch (Exception e) {
            failStopWithScreenshot("Valid login validation failed: ${e.getClass().getSimpleName()} - ${e.getMessage()}")
        } finally {
            try {
                WebUI.closeBrowser()
            } catch (Exception ignored) {
            }
        }
    }

    @Keyword
    def loginWithInvalidPassword() {
        String randomPassword = UUID.randomUUID().toString().substring(0, 8)

        try {
            commonLogin(GlobalVariable.USERNAME, randomPassword)

            // Validate invalid password error
            waitPresentAndVisible('pages/login/invalidPassword_error', DEFAULT_TIMEOUT)

            // Ensure user is NOT logged in
            boolean loggedIn = WebUI.waitForElementPresent(findTestObject('pages/login/successful_login'), 5)
            if (loggedIn) {
                failStopWithScreenshot('Unexpected successful login detected with invalid password (should NOT be logged in).')
            }
        } catch (Exception e) {
            failStopWithScreenshot("Invalid password validation failed: ${e.getClass().getSimpleName()} - ${e.getMessage()}")
        } finally {
            try {
                WebUI.closeBrowser()
            } catch (Exception ignored) {
            }
        }
    }
}
