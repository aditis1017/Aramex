import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_Detailed_30
// Steps: Login → Customs Search → Search (no tracking number) → Shipment row → History → change page size to 10

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyPaginationCorrectRecordsPerPageSize()
WebUI.takeScreenshot()
WebUI.closeBrowser()
