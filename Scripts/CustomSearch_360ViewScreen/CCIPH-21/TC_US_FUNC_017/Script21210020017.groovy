import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-21 | TC_US_FUNC_017

def cs = new CS()
def login = new MSLogin()

long exportStart = System.currentTimeMillis()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectFiltersById(
	'ccfe-customs-charge-code': 'ECD',
	'ccfe-customs-charge-status': 'Actualized'
)
cs.clickSearch()
cs.verifySearchResultsDisplayed()

String uiRowText = cs.getFirstSearchResultRowText()
cs.prepareCustomSearchExportDownloadDirectory()
cs.clickExportButton()

String exportPath = cs.verifyLatestCustomSearchExportDownloaded(exportStart)
KeywordUtil.logInfo("Custom Search export downloaded to: ${exportPath}")

cs.verifyExportedCustomSearchFilePresentInDownloads(exportPath)
cs.verifyExportedCustomSearchFileHasColumnHeaders(exportPath)
cs.verifyExportedCustomSearchFileContainsAppliedFilters(exportPath, [
	'Charge Code'   : 'ECD',
	'Status' : 'Actualized'
])
if (uiRowText) {
	cs.verifyExportedCustomSearchFileContainsUiRowSnippet(exportPath, uiRowText)
}
cs.deleteLastCustomSearchExportFile(exportPath)

WebUI.takeScreenshot()
WebUI.closeBrowser()
