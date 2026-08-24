import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import b2bshipment.BusinessActions as B2B
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_014 | Extra columns ignored

def login = new MSLogin()
def b2b = new B2B()

'This is for Login Profile'
login.loginToCustomsManagementUsingAutomationProfile()
'B2B Keywords Manager / classification steps'
b2b.navigateToB2BKeywordsManager()
WebUI.takeScreenshot()
String kw = "ExtraColKW01_${System.currentTimeMillis()}"
String path = b2b.createB2BKeywordsExcel([[kw, 'Contains', 'IgnoreMe']], 'extra_columns.xlsx', ['ExtraColumn'])
b2b.uploadB2BExcelFile(path)
b2b.verifySubmitEnabled()
b2b.clickSubmit()
b2b.verifyUploadSuccessOrTableRefresh()
b2b.verifyKeywordRow(kw, 'Contains')
TestObject thExtra = new TestObject('th_extra')
thExtra.addProperty('xpath', ConditionType.EQUALS, "//th[normalize-space()='ExtraColumn']")
WebUI.verifyElementNotPresent(thExtra, 5, FailureHandling.OPTIONAL)
WebUI.takeScreenshot()
WebUI.closeBrowser()
