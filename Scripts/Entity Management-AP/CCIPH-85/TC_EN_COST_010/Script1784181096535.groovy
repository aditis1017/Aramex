import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_010'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_010

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
//em.openEntitySettingsCostApChargeType(entity, charge_Type)
em.openEntitySettingsCostAp(entity)
em.clickEntitySelectionContinue()
em.verifyAllCostApInvoicingFieldsForChargeType(entity, charge_Type)
if ((entry_Limit_Value ?: '').trim()) {
	em.enterEntryLimitValue(entry_Limit_Value)
}
if ((sap_Company_Code ?: '').trim()) {
	em.enterSapCompanyCodeValue(sap_Company_Code)
}
em.saveEntityInvoicingConfigurationAndVerify()
WebUI.takeScreenshot()
WebUI.closeBrowser()

