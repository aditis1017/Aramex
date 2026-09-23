// CSV: Upload Estimate_Actuals.csv | CCIPH-60 | TC_ACT_EST_014 | Verify eligible cost equals estimate when total actual is 0
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
String entity = (binding.hasVariable('entity_Name') && entity_Name) ? entity_Name as String : 'RUH'
String estimateAmount = (binding.hasVariable('estimate_Amount') && estimate_Amount) ? estimate_Amount as String : '1000'
String eligibleCost = (binding.hasVariable('eligible_Cost') && eligible_Cost) ? eligible_Cost as String : estimateAmount

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.uploadEstimateForShipment(awb, estimateAmount, entity)
cs.verifyEcdInvoicingTableUI(awb)
uea.verifyEligibleCostEqualsEstimateWhenNoActual(eligibleCost)
WebUI.takeScreenshot()
WebUI.closeBrowser()
