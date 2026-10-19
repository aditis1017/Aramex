import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CCIPH-560 | AR_TYPE006 — Reset one dropdown (Select); other mandatory / read-only values unchanged (Stage)

final String RESET_DROPDOWN = 'Customs Type'
final String SEARCH_ACCOUNT = '75800042'
final String SEARCH_ENTITY = 'RUH'

final Map<String, String> mandatoryDropdowns = [
	'Rate Currency'       : 'SAR',
	'Billing Currency'    : 'SAR',
	'Customs Type'        : 'DDP',
	'Eligible For Billing': 'Yes',
	'Invoice Trigger'     : 'Manual',
]

final List<String> readonlyFieldsToSnapshot = [
	'Account Name',
	'Account Number',
	'Country',
	'Entity',
	'Account Type',
	'Account Business Type',
	'Business Segment',
]

def login = new MSLogin()
def ar_management = new AR()

'Step 1–2 — Stage URL + Customs Manager login (automation profile)'
login.loginToStageCustomsManagementUsingAutomationProfile()
ar_management.navigateToStageArManagement()

'Step 3 — Open existing AR account in edit mode (DTA)'
ar_management.openArAccountDetailOnStageForEditCciph560V2('Account Number', 'Contains', SEARCH_ACCOUNT, SEARCH_ENTITY)

'Step 4 — Populate mandatory dropdowns with known values'
ar_management.populateArAccountMandatoryDropdownsFromMapV2(mandatoryDropdowns)

Map<String, String> dropdownSnapshot = ar_management.captureArAccountDetailFieldValuesV2(
	mandatoryDropdowns.keySet().toList()
)
Map<String, String> readonlySnapshot = ar_management.captureArAccountDetailFieldValuesV2(readonlyFieldsToSnapshot)

'Step 5 — Reset Customs Type via Select'
ar_management.selectArAccountDetailsDropdownSelectOptionV2(RESET_DROPDOWN)

'Step 6 — Save'
ar_management.clickSaveAccountInformation()

'Step 7 — Reopen record'
ar_management.clickBackButton()
ar_management.clickFirstArAccountNumberLink()
ar_management.verifyArDtaPageLoaded()

'Step 8 — Only reset dropdown cleared; others retain prior values'
ar_management.verifyArAccountDetailsDropdownShowsSelectPlaceholderV2(RESET_DROPDOWN)
ar_management.verifyArAccountDetailFieldValuesMatchSnapshotExceptV2(dropdownSnapshot, RESET_DROPDOWN)
ar_management.verifyArAccountDetailFieldValuesMatchSnapshotV2(readonlySnapshot)

WebUI.takeScreenshot()
WebUI.closeBrowser()
