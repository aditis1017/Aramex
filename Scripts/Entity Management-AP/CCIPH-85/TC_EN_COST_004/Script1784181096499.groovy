import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def tcId = 'TC_EN_COST_004'
// CSV: Entity-Management-CCIPH85-Data.csv | CCIPH-85 | TC_EN_COST_004

def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
//em.verifyInvoicingFieldForChargeType(entity, charge_Type, field_Label, true)
em.openEntitySettingsCostAp(entity)
em.clickEntitySelectionContinue()
//em.verifyInvoicingSettingsVisibleForAllCostChargeTypes()
//em.verifyInvoicingFieldForChargeType(entity, charge_Type, field_Label, true)
//em.verifyEntitySettingField("split-entry", "BOE")
//em.verifyEntitySettingField("account","FIXED")
//em.verifyEntitySettingField( "country","SA")
//em.verifyEntitySettingField("entity", "RUH")
//em.verifyEntitySettingField("entry-limit", "10000")
em.verifyDropdownField("split-entry", ["BOE", "Payment Referenced"], "BOE")
em.verifyDropdownField("cost-account", ["AMENDABLE", "FIXED"], "AMENDABLE")
em.toggleDropdownValue("split-entry", "BOE", "Payment Referenced")
em.clickSaveButton()
em.saveButtonPopUp()


//em.verifyInputField("country", "SA")
//em.verifyInputField("entry-limit", "10000")

WebUI.takeScreenshot()
WebUI.closeBrowser()


