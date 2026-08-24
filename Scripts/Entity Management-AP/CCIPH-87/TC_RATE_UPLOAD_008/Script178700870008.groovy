// CSV: Entity Management-AR-TestCases.csv | CCIPH-87 | TC_RATE_UPLOAD_008 | AC2 (DB out of browser)
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
em.uploadValidEntityRateSheetAndVerifySaved(valid_Upload_Path, row_Fragment ?: 'AUTO RS Desc')
em.verifyEntityRateSheetUploadedDataDbReminder()
WebUI.takeScreenshot()
WebUI.closeBrowser()
