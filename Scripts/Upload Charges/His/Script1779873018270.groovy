import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import globalsettings.BusinessActions as GS
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

// CSV: Global Settings - Sheet1.csv | CCIPH-37 | TC_GS_FUNC_076 — refresh restores default pagination
WebUI.openBrowser('')

WebUI.navigateToUrl('https://dev-cc.aramex.com/customs-management/global-settings')

WebUI.click(findTestObject('History-Tab/Page_Custom Charges App/button_Sign in with Microsoft'))

WebUI.setText(findTestObject('History-Tab/Page_Sign in to your account/input_UserIDaramex.com'), 'aditi.singh1@tothenew.com')

WebUI.sendKeys(findTestObject('History-Tab/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))

WebUI.setText(findTestObject('History-Tab/Page_Sign in to your account/input_Enter code'), '17569595')

WebUI.click(findTestObject('History-Tab/Page_Sign in to your account/input_idSIButton9'))

WebUI.click(findTestObject('History-Tab/Page_Custom Charges App/span_Global Settings'))

WebUI.click(findTestObject('History-Tab/Page_Custom Charges App/button_HISTORY'))

WebUI.click(findTestObject('History-Tab/Page_Global Settings History  Customs Management/h1_Global Settings History'))

WebUI.rightClick(findTestObject('History-Tab/Page_Global Settings History  Customs Management/h1_Global Settings History'))

WebUI.verifyElementText(findTestObject('History-Tab/Page_Global Settings History  Customs Management/h1_Global Settings History'), 
    'Global Settings History')
