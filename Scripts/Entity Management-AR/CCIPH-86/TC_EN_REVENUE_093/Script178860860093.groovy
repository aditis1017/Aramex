// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_093 | AC1
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import customsmanagement.MicrosoftLoginKeywords as MSLogin
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
def login = new MSLogin()

'This is for Login Profile'
login.loginToCustomsManagementUsingProfile()
'This is used for Navigating to Entity Settings'
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.verifyEntityRateSheetSettingsTableColumnsFromData(rs_Columns)
WebUI.takeScreenshot(); WebUI.closeBrowser()

