// CSV: Entity Management AP- TestCases.xlsx | CCIPH-37 | TC_COST_HS_015
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import entitymanagement.BusinessActions as EM
import globalsettings.BusinessActions as GS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

def em = new EM()
def gs = new GS()
def login = new MSLogin()
String sizeLabel = (page_Size ?: '10').toString().trim()
login.loginToCustomsManagementUsingProfile()
em.openEntitySettingsHistoryFromLanding()
gs.clickHistoryDateFilter(history_Filter ?: 'ALL')
gs.selectHistoryPageSizeAndVerifyMaxVisibleRows(sizeLabel)
WebUI.takeScreenshot()
WebUI.closeBrowser()
