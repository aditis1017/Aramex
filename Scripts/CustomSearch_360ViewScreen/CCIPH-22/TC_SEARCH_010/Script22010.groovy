import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-22 | TC_SEARCH_010 | Verify search using multiple filter combinations

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.setAdditionalDetailsToggle(true)
//cs.verifyCombinedBasicAndAdditionalSearch([
//    charge_Country      : charge_Country,
//    entity              : entity,
//    charge_Group        : charge_Group,
//    charge_Code         : charge_Code,
//    charge_Status       : charge_Status,
//
//    account_Type        : account_Type,
//    business_Type       : business_Type,
//    category            : category,
//    search_Criteria     : search_Criteria,
//    origin_Entity       : origin_Entity,
//    origin_Country      : origin_Country,
//    dest_Entity         : dest_Entity,
//    dest_Country        : dest_Country,
//    third_Party_Entity  : third_Party_Entity,
//    third_Party_Country : third_Party_Country
//])
cs.selectDropdownValueList("charge-status", charge_Status)
cs.selectDropdownValueList("charge-group", charge_Group)
cs.selectDropdownValueList("charge-code", charge_Code)
cs.selectDropdownValueList("account-type", account_Type)
cs.selectDropdownValueList("business-type", business_Type)
cs.selectDropdownValueList("category", category)
cs.selectDropdownValueList("origin-country", origin_Country)
cs.selectDropdownValueList("origin-entity", origin_Entity)
cs.selectDropdownValueList("dest-country", dest_Country)
cs.selectDropdownValueList("dest-entity", dest_Entity)
cs.selectDropdownValueList("third-country", third_Party_Country)
cs.selectDropdownValueList("third-entity", third_Party_Entity)
cs.clickSearch()
cs.verifySearchResultsDisplayed()

WebUI.takeScreenshot()
WebUI.closeBrowser()
