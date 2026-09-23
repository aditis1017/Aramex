// CSV: Upload Estimate_Actuals.csv | CCIPH-60 | TC_ACT_EST_005 | Verify total actual cost is sum of all uploaded actuals
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
String actualAmount2 = (binding.hasVariable('actual_Amount_2') && actual_Amount_2) ? actual_Amount_2 as String : '200'
String actualAmount3 = (binding.hasVariable('actual_Amount_3') && actual_Amount_3) ? actual_Amount_3 as String : '300'

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
uea.uploadMultipleActualsForShipment(awb, [actualAmount1, actualAmount2, actualAmount3], entity)
cs.verifyEcdInvoicingTableUI(awb)
uea.verifyTotalActualCostDisplayed((new BigDecimal(actualAmount1) + new BigDecimal(actualAmount2) + new BigDecimal(actualAmount3)).toString())
WebUI.takeScreenshot()
WebUI.closeBrowser()
