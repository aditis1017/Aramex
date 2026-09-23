// CSV: Upload Estimate_Actuals.csv | CCIPH-60 | TC_ACT_EST_016 | Verify outstanding value becomes negative when invoiced value exceeds eligible cost
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Upload_Estimate_Actuals.BusinessActions as UEA
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsearch_360viewscreen.BusinessActions as CS

def login = new MSLogin()
def uea = new UEA()
def cs = new CS()

String awb = (binding.hasVariable('valid_AWB') && valid_AWB) ? valid_AWB as String : 'B2BSHIPDATA200'
String invoicedValue = (binding.hasVariable('invoiced_Value') && invoiced_Value) ? invoiced_Value as String : '5000'
String eligibleCost = (binding.hasVariable('eligible_Cost') && eligible_Cost) ? eligible_Cost as String : '3000'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyDtcInvoicingTableUI(awb)
uea.verifyNegativeOutstandingHandling(eligibleCost, invoicedValue)
WebUI.takeScreenshot()
WebUI.closeBrowser()
