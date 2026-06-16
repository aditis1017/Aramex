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

WebUI.navigateToUrl('https://dev-cc.aramex.com/customs-management')

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_Sign in with Microsoft'))

WebUI.setText(findTestObject('Percentage/Page_Sign in to your account/input_UserIDaramex.com'), 'aditi.singh1@tothenew.com')

WebUI.sendKeys(findTestObject('Percentage/Page_Sign in to your account/input_UserIDaramex.com'), Keys.chord(Keys.ENTER))

WebUI.setText(findTestObject('Percentage/Page_Sign in to your account/input_Enter code'), '19117934')

WebUI.click(findTestObject('Percentage/Page_Sign in to your account/input_idSIButton9'))

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/div_AR Management'))

WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_Enter Account Number'), '8001')

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_SEARCH'))

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/span_8001'))

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/rect_rect'))

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/button_Add new'))

WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-description'), 'test')

WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-col-service-description'), 'stest')

WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-col-service-shipment-type'), 
    'DDP', false)

WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-col-rate-type'), 'Percentage', 
    false)

WebUI.selectOptionByValue(findTestObject('Percentage/Page_Custom Charges App/select_entity-settings-percentage-of-inline'), 
    'Cost of Goods', false)

WebUI.setText(findTestObject('Percentage/Page_Custom Charges App/input_entity-settings-col-rate-1'), '35')

WebUI.click(findTestObject('Percentage/Page_Custom Charges App/path_path'))

