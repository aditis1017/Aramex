package percentage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.Keys

public class PercentageKeywords {

    @Keyword
    def openCustomChargesApp() {
        WebUI.openBrowser('')
        WebUI.navigateToUrl('https://dev-cc.aramex.com/customs-management')
    }

    @Keyword
    def clickSignInWithMicrosoft() {
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_Sign in with Microsoft'))
    }

    @Keyword
    def enterUserId(String userId) {
        WebUI.setText(findTestObject('Percentage/Page_Sign in to your account/input_UserIDaramex.com'), userId)
        WebUI.sendKeys(findTestObject('Percentage/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))
    }

    @Keyword
    def enterCodeAndContinue(String code) {
        WebUI.setText(findTestObject('Percentage/Page_Sign in to your account/input_Enter code'), code)
        WebUI.click(findTestObject('Percentage/Page_Sign in to your account/input_idSIButton9'))
    }

    @Keyword
    def openArManagement() {
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/div_AR Management'))
    }

    @Keyword
    def searchAccount(String accountNumber) {
        WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_Enter Account Number'), accountNumber)
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_SEARCH'))
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/span_8001'))
    }

    @Keyword
    def openSettings() {
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/rect_rect'))
    }

    @Keyword
    def clickAddNew() {
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_Add new'))
    }

    @Keyword
    def fillPercentageForm(String description, String serviceDescription, String shipmentType, String rateType, String percentageOf, String rate) {
        WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-description'), description)
        WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-col-service-description'), serviceDescription)
        WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-col-service-shipment-type'), shipmentType, false)
        WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-col-rate-type'), rateType, false)
        WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-percentage-of-inline'), percentageOf, false)
        WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-col-rate-1'), rate)
    }

    @Keyword
    def savePercentageEntry() {
        WebUI.click(findTestObject('Percentage/Page_Custom Charges App/path_path'))
    }
}
