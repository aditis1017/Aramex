import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ap_ar_management.BusinessActions as AR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CCIPH-560 | AR_TYPE005 — Customs Type: save value, change option, clear via Select, verify reset

def login = new MSLogin()
def ar_management = new AR()

'Login and open AR Management (profile / dev URL via automation profile)'
login.loginToCustomsManagementUsingAutomationProfile()
ar_management.navigateToArManagement()
ar_management.verifyArSearchPageLoaded()
ar_management.performArSearch('Account Number', 'Contains', '75800042', 'RUH')
ar_management.clickFirstArAccountNumberLink()
ar_management.verifyArDtaPageLoaded()
//ar_management.selectDropdownValueByText('Customs Type', 'DDP')
//ar_management.clickSaveAccountInformation()
ar_management.selectArAccountDetailsDropdownValueByTextV2('Rate Currency', 'Select')
ar_management.verifyArAccountDetailsDropdownSelectOptionAtTopV2('Rate Currency')
ar_management.selectArAccountDetailsDropdownSelectOptionV2('Rate Currency')
ar_management.clickSaveAccountInformation()
ar_management.clickBackButton()
ar_management.clickFirstArAccountNumberLink()
//ar_management.verifyArAccountDetailsDropdownShowsSelectPlaceholderV2('Customs Type')
ar_management.verifyArAccountDetailsDropdownShowsSelectPlaceholderV2('Rate Currency')
//ar_management.selectArAccountDetailsDropdownFirstNonEmptyOptionV2('Rate Currency', 'SAR')
WebUI.takeScreenshot()
WebUI.closeBrowser()
