// CSV: Entity Management-AR-TestCases.csv | CCIPH-87 | TC_RATE_UPLOAD_001 | AC1
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
long started = System.currentTimeMillis()
em.clickEntityRateSheetDownloadTemplate()
String path = em.verifyEntityRateSheetTemplateDownloaded(started, null)
WebUI.verifyNotEqual(path, '', com.kms.katalon.core.model.FailureHandling.STOP_ON_FAILURE)
WebUI.takeScreenshot()
em.deleteLastEntityRateSheetTemplateFile(path)
WebUI.closeBrowser()
