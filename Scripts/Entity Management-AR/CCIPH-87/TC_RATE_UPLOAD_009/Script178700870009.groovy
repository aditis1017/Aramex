// CSV: Entity Management-AR-TestCases.csv | CCIPH-87 | TC_RATE_UPLOAD_009 | AC1/AC2 � Jira scope ECD/DTA/DTC
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def em = new EM()
em.navigateToEntitySettings()
List<String> charges = ((charge_Types_List ?: 'ECD|DTA|DTC') as String).split(/\|/).collect { it.trim() }.findAll { it }
em.verifyEntityRevenueRateSheetUploadAcrossChargeTypes(entity, charges, valid_Upload_Path)
WebUI.takeScreenshot()
WebUI.closeBrowser()
