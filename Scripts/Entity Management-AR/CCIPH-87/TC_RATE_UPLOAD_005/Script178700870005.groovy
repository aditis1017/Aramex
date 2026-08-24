// CSV: Entity Management-AR-TestCases.csv | CCIPH-87 | TC_RATE_UPLOAD_005 | AC2
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.clickEntityRateSheetUpload()
em.uploadEntityRateSheetFile(missing_Cols_Path)
em.clickEntityRateSheetUploadSubmit()
em.verifyEntityRateSheetUploadErrorVisible()
WebUI.takeScreenshot()
WebUI.closeBrowser()
