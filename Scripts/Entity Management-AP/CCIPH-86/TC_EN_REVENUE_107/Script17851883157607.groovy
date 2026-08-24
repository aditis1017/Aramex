// CSV: Entity Management-AR-TestCases.csv | CCIPH-86 | TC_EN_REVENUE_107 | AC5 — AR Management Special Rate Settings
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
// Precondition: authenticated session (LOGIN-TEST_SUITE / profile) - login not executed in feature script.
def ar = new AR()
def rateVal = String.valueOf(System.currentTimeMillis() % 100000)
KeywordUtil.logInfo('TC_EN_REVENUE_107 uses AR Management Special Rate Settings (CSV steps)')
ar.navigateToArManagement()
ar.verifyArSearchPageLoaded()
// Preconditions: bind AR search vars from profile/data when available; fallback opens first searchable path via existing keyword signature
try {
  ar.openArAccountDetailAndRatesheetSettings(search_Criteria, search_By, search_Value, entity)
} catch (MissingPropertyException ignored) {
  KeywordUtil.logInfo('AR search variables not bound — ensure AR-Management-Data linked or set search_Criteria/search_By/search_Value/entity')
  throw ignored
}
ar.clickArRatesheetAddNew()
ar.fillArRatesheetNewRowFromData(serviceCode ?: 'SVC001', rs_Shipment_Type ?: 'DDU', rs_Rate_Type ?: 'Fixed', rateVal)
ar.clickArRatesheetSaveRow()
ar.clickArRatesheetRate1ValueLinkForRow(rateVal)
ar.verifyArRatesheetRateTypeDropdownOptions()
WebUI.takeScreenshot()
ar.clickArRatesheetCreationBack()
ar.clickArRatesheetDeleteRowByDescription(rateVal)
ar.confirmArRatesheetDeletePopup()
WebUI.closeBrowser()
