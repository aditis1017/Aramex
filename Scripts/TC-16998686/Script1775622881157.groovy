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

//import io.qameta.allure.TmsLink
//@TmsLink("TC-16998686"/)


//@TestCaseId("TC-16998686")
//def Login() {
WebUI.openBrowser('')

WebUI.navigateToUrl('https://practicetestautomation.com/practice-test-login/')

WebUI.setText(findTestObject('TC-16998686/Page_Test Login  Practice Test Automation/input_Username'), 'student')

WebUI.setEncryptedText(findTestObject('TC-16998686/Page_Test Login  Practice Test Automation/input_Password'), 'p4y+y39Ir5PJb2ispxT0Ew==')

WebUI.click(findTestObject('TC-16998686/Page_Test Login  Practice Test Automation/button_submit'))

WebUI.rightClick(findTestObject('TC-16998686/Page_Logged In Successfully  Practice Test _c87e4f/h1_Logged In Successfully'))

WebUI.verifyElementText(findTestObject('TC-16998686/Page_Logged In Successfully  Practice Test _c87e4f/h1_Logged In Successfully'), 
    'Logged In Successfully')

WebUI.click(findTestObject('TC-16998686/Page_Logged In Successfully  Practice Test _c87e4f/a_Log out'))

WebUI.rightClick(findTestObject('TC-16998686/Page_Test Login  Practice Test Automation/h2_Test login'))

WebUI.verifyElementPresent(findTestObject('TC-16998686/Page_Test Login  Practice Test Automation/h2_Test login'), 0)

WebUI.closeBrowser()
//}
