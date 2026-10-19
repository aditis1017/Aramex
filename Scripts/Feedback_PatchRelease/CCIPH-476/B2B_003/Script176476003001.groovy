import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import Feedback_PatchRelease.BusinessActions as FPR
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: NewFeedback_PatchChanges.csv | User Story: CCIPH-476 | ID: B2B-003
// Jira: https://aramex.atlassian.net/browse/CCIPH-476
// Hardcoded values (no Data Files / variable binding) so the case can run without binding errors.
// Set awb_* below after shipment ingestion to enable 360 View classification checks.

def login = new MSLogin()
def fpr = new FPR()

Map<String, String> testData = [
	exact_Keyword              : 'ARAMEX LLC',
	exact_MatchType            : 'Exact',
	consignee_Exact            : 'ARAMEX LLC',
	awb_Exact                  : '',
	contains_Keyword           : 'TRADING',
	contains_MatchType         : 'Contains',
	consignee_Contains         : 'Global TRADING Company',
	awb_Contains               : '',
	start_Keyword              : 'GLOBAL',
	start_MatchType            : 'Start With',
	consignee_StartWith        : 'GLOBAL Trading Company',
	awb_StartWith              : '',
	end_Keyword                : 'CORP',
	end_MatchType              : 'End With',
	consignee_EndWith          : 'ARAMEX CORP',
	awb_EndWith                : '',
	filter_Category            : 'Low Value (LV)',
	filter_CustomsType         : 'DDU',
	filter_ShipmentBusinessType: 'B2B',
	filter_ChargeStatus        : 'Pending Calculation'
]

'Login via automation profile (CSV login steps are preconditions only)'
login.loginToStageCustomsManagementUsingAutomationProfile()
WebUI.takeScreenshot()

'B2B-003: upload keywords (Exact, Contains, Start With, End With); 360 View when AWBs are hardcoded'
fpr.executeB2B003MatchTypeClassificationFlow(testData)

WebUI.takeScreenshot()
WebUI.closeBrowser()
