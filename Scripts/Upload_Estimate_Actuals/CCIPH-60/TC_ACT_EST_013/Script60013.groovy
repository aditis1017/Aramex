// CSV: Upload Estimate_Actuals.csv | CCIPH-60 | TC_ACT_EST_013 | Verify eligible cost equals total actual cost when actual exists
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'
String actualAmount1 = (binding.hasVariable('actual_Amount_1') && actual_Amount_1) ? actual_Amount_1 as String : '100'
String eligibleCost = (binding.hasVariable('eligible_Cost') && eligible_Cost) ? eligible_Cost as String : ''

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.uploadSingleActualForShipment(awb, actualAmount1, entity)
uea.clickBackButton()
cs.verifyEcdInvoicingTableUI(awb)
uea.verifyEligibleCostDisplayed(eligibleCost ?: actualAmount1)
WebUI.takeScreenshot()
WebUI.closeBrowser()
