import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import ChargeCreation.BusinessActions as CC
import entitymanagement.BusinessActions as EM

// ECS Validation

def cs = new CS()
def login = new MSLogin()
def charge = new CC()
def em = new EM()


long exportStart = System.currentTimeMillis()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.selectFiltersById(
    'ccfe-customs-charge-group': 'Cost',
	'ccfe-customs-charge-code': 'ECS'

)

cs.enterTrackingNumber("3002298710093")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
charge.verifyDetailFieldValue("Payment Type", "P")

String originSection = 'ORIGIN'
String originEntity = cs.readAndStoreSectionFieldValue(originSection, 'Entity')
cs.verifySectionAndFields(
	originSection,
	[
		"Entity": originEntity
	]
)

String destEntity = cs.readAndStoreSectionFieldValue('DESTINATION', 'Entity')

em.navigateToStageEntitySettings()
em.verifyEntitySettingsPageLoaded()
em.selectCountry('United States')
em.selectEntity(originEntity)
//em.selectEntity("JFK")
em.clickEntitySelectionContinue()
em.clickCostApSection()
em.selectChargeType('ECS')
//em.verifyEcsEc3ServiceCodeMappingSectionVisible()
em.verifyEntitySettingsSection("Service Codes Mapping")
em.selectEntityByText("RUH")


Map<String, List<String>> storedMappings =
em.readEntityMapMappings()
List<String> customsDutyOutboundMappings =
storedMappings["customs duty outbound"]

List<String> mappings =
em.verifyStoredEntityMapMapping(
	storedMappings,
	"Customs Duty Outbound"
)

String serviceDescription =
"Customs Duty Inbound"

boolean found =
mappings.any { String actual ->
	actual.equalsIgnoreCase(serviceDescription)
}

if (!found) {
KeywordUtil.markFailedAndStop(
	"Customs Duty Inbound is not mapped to " +
	"Customs Duty Outbound"
)
}
cs.navigateToStageCustomsSearchScreen()
cs.enterTrackingNumber("3002298710093")
cs.clickSearch()
cs.verifySearchResultsDisplayed()
cs.clickFirstSearchResultRow()
charge.selectChargeTypeTab('DTA')

WebUI.takeScreenshot()
//WebUI.closeBrowser()