// CSV: Entity Management-AR-TestCases.csv | CCIPH-87 | TC_RATE_UPLOAD_002 | AC1
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
em.openEntitySettingsRevenueArRateSheetSettings(entity, charge_Type ?: 'DTC')
long started = System.currentTimeMillis()
em.clickEntityRateSheetDownloadTemplate()
String path = em.verifyEntityRateSheetTemplateDownloaded(started, null)
em.verifyEntityRateSheetTemplateColumnsAndSampleData(path, rs_Columns)
WebUI.takeScreenshot()
em.deleteLastEntityRateSheetTemplateFile(path)
WebUI.closeBrowser()
