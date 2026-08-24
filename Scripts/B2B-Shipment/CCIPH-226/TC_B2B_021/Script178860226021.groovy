//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//import b2bshipment.BusinessActions as B2B
//import customsmanagement.MicrosoftLoginKeywords as MSLogin
//
//// CSV: B2B-Keywords-Manager-TestCases.csv.csv | CCIPH-226 | TC_B2B_021 | Drag-drop staging (file input equivalent)//
//// Note: Katalon uses native file input upload as the stable equivalent of drag-and-drop staging.
//
//def login = new MSLogin()
//def b2b = new B2B()
//
//'This is for Login Profile'
//login.loginToStageCustomsManagementUsingAutomationProfile()
//'Navigate to B2B Keywords Manager'
//b2b.navigateToStageB2BKeywordsManager()
//WebUI.takeScreenshot()
//String path = b2b.createB2BKeywordsExcel([["DragKW_${System.currentTimeMillis()}", 'Contains']], 'drag_drop_stage.xlsx')
//b2b.uploadB2BExcelFile(path)
//b2b.verifySubmitEnabled()
//WebUI.takeScreenshot()
//WebUI.closeBrowser()
