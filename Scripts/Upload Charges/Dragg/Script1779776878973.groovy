import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

WebUI.openBrowser('')

WebUI.navigateToUrl('https://dev-cc.aramex.com/login')

WebUI.setText(findTestObject('Drag/Page_Sign in to your account/input_UserIDaramex.com'), 'aditi.singh1@tothenew.com')

WebUI.sendKeys(findTestObject('Drag/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))

WebUI.setText(findTestObject('Drag/Page_Sign in to your account/input_Enter code'), '13786522')

WebUI.click(findTestObject('Drag/Page_Sign in to your account/input_idSIButton9'))

WebUI.click(findTestObject('Drag/Page_Custom Charges App/span_Global Settings'))

WebUI.click(findTestObject('Drag/Page_Custom Charges App/button_Reorder Rules'))

WebUI.click(findTestObject('Drag/Page_Custom Charges App/td_1'))

WebUI.click(findTestObject('Drag/Page_Custom Charges App/button_Save Order'))

