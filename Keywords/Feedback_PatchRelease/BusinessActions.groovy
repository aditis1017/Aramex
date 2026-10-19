package Feedback_PatchRelease

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import b2bshipment.BusinessActions as B2B
import customsearch_360viewscreen.BusinessActions as CustomSearch
import ChargeCreation.BusinessActions as CC
import Upload_Estimate_Actuals.BusinessActions as UEA

/**
 * Feedback Patch Release — CCIPH-476 B2B keyword classification and CCIPH-549
 * Declaration Number → BOE1/BOE2/BOE3/Additional mapping on Upload Actuals / 360 View.
 * Reuses B2B-Shipment, Custom Search, Charge Detail, and Upload_Estimate_Actuals keywords.
 */
class BusinessActions {

	private static final int DEFAULT_TIMEOUT = 30
	private static final String OR_B2B_PREFIX = 'B2B-Shipment/CCIPH-226/'
	private static final String OR_BOE_PREFIX = 'Feedback_PatchRelease/CCIPH-549/'

	private final B2B b2b = new B2B()
	private final CustomSearch customSearch = new CustomSearch()
	private final CC chargeCreation = new CC()
	private final UEA uea = new UEA()

	private static void failStop(String message) {
		try {
			WebUI.takeScreenshot()
		} catch (Throwable ignored) {
		}
		KeywordUtil.markFailedAndStop(message)
	}

	@Keyword
	def uploadB2BKeywordRow(String keyword, String matchType) {
		try {
			String kw = (keyword ?: '').trim()
			String mt = (matchType ?: '').trim()
			if (!kw || !mt) {
				failStop('uploadB2BKeywordRow: keyword and matchType are required')
			}
			b2b.navigateToStageB2BKeywordsManager()
			WebUI.takeScreenshot()
			b2b.uploadValidAndSubmit([[kw, mt]])
			b2b.verifyKeywordRow(kw, mt)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadB2BKeywordRow(${keyword}, ${matchType}) failed: ${e.message}")
		}
	}

	@Keyword
	def applyB2B003CustomSearchFilters(
			String category = 'Low Value (LV)',
			String customsType = 'DDU',
			String shipmentBusinessTypeFilter = 'B2B',
			String chargeStatus = 'Pending Calculation') {
		try {
			customSearch.navigateToStageCustomsSearchScreen()
			Map<String, String> filters = [
				'Category'      : (category ?: '').trim(),
				'Customs Type'  : (customsType ?: '').trim(),
				'Charge Status' : (chargeStatus ?: '').trim()
			]
			customSearch.selectFilters(filters)
			String sbt = (shipmentBusinessTypeFilter ?: '').trim()
			if (sbt) {
				selectShipmentBusinessTypeFilter(sbt)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("applyB2B003CustomSearchFilters failed: ${e.message}")
		}
	}

	private void selectShipmentBusinessTypeFilter(String value) {
		List<String> labels = ['Shipment Business Type', 'Business Type']
		for (String label : labels) {
			try {
				customSearch.selectDropdownByLabel(label, value)
				KeywordUtil.logInfo("Selected '${value}' on filter '${label}'")
				WebUI.takeScreenshot()
				return
			} catch (Exception ignored) {
				KeywordUtil.logInfo("Filter label '${label}' not used: ${ignored.message}")
			}
		}
		failStop("Could not select Shipment Business Type filter value '${value}'")
	}

	@Keyword
	def verifyShipmentBusinessTypeOn360View(
			String trackingNumber,
			String expectedBusinessType,
			String category = 'Low Value (LV)',
			String customsType = 'DDU',
			String shipmentBusinessTypeFilter = 'B2B',
			String chargeStatus = 'Pending Calculation') {
		try {
			String tracking = (trackingNumber ?: '').trim()
			String expected = (expectedBusinessType ?: '').trim()
			if (!tracking) {
				KeywordUtil.markWarning(
					'trackingNumber is blank — skip 360 Shipment Business Type check. ' +
					'Populate hardcoded awb_* values in Scripts/Feedback_PatchRelease/CCIPH-476/B2B_003 after ingestion.'
				)
				WebUI.takeScreenshot()
				return
			}
			if (!expected) {
				failStop('verifyShipmentBusinessTypeOn360View: expectedBusinessType is required')
			}
			applyB2B003CustomSearchFilters(category, customsType, shipmentBusinessTypeFilter, chargeStatus)
			customSearch.enterTrackingNumber(tracking)
			customSearch.clickSearch()
			customSearch.verifySearchResultsDisplayed()
			customSearch.clickFirstSearchResultRow()
			verifyShipmentBusinessTypeField(expected)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyShipmentBusinessTypeOn360View(${trackingNumber}, ${expectedBusinessType}) failed: ${e.message}")
		}
	}

	private void verifyShipmentBusinessTypeField(String expected) {
		List<String> fieldLabels = ['Shipment Business Type', 'Business Type']
		for (String label : fieldLabels) {
			try {
				chargeCreation.verifyDetailFieldValue(label, expected)
				KeywordUtil.logInfo("Verified '${label}' = '${expected}' on Charge Detail 360 View")
				return
			} catch (Exception ignored) {
				KeywordUtil.logInfo("Field '${label}' verify attempt: ${ignored.message}")
			}
		}
		failStop("Shipment Business Type '${expected}' not found on 360 View (tried: ${fieldLabels})")
	}

	/**
	 * CCIPH-476 / CSV B2B-003 — keyword upload + optional 360 classification per match type.
	 * Shipment ingestion is a data precondition (InfoAXS / standard feed); AWBs hardcoded in the test script.
	 */
	@Keyword
	def executeB2B003MatchTypeClassificationFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String category = pick(d, 'filter_Category', 'Low Value (LV)')
			String customsType = pick(d, 'filter_CustomsType', 'DDU')
			String chargeStatus = pick(d, 'filter_ChargeStatus', 'Pending Calculation')
			String sbtFilter = pick(d, 'filter_ShipmentBusinessType', 'B2B')

			List<Map> scenarios = [
				[
					step       : 'Exact',
					keyword    : pick(d, 'exact_Keyword', 'ARAMEX LLC'),
					matchType  : pick(d, 'exact_MatchType', 'Exact'),
					awb        : pick(d, 'awb_Exact', ''),
					consignee  : pick(d, 'consignee_Exact', 'ARAMEX LLC'),
					expectedSbt: 'B2B'
				],
				[
					step       : 'Contains',
					keyword    : pick(d, 'contains_Keyword', 'TRADING'),
					matchType  : pick(d, 'contains_MatchType', 'Contains'),
					awb        : pick(d, 'awb_Contains', ''),
					consignee  : pick(d, 'consignee_Contains', 'Global TRADING Company'),
					expectedSbt: 'B2B'
				],
				[
					step       : 'Start With',
					keyword    : pick(d, 'start_Keyword', 'GLOBAL'),
					matchType  : pick(d, 'start_MatchType', 'Start With'),
					awb        : pick(d, 'awb_StartWith', ''),
					consignee  : pick(d, 'consignee_StartWith', 'GLOBAL Trading Company'),
					expectedSbt: 'B2B'
				],
				[
					step       : 'End With',
					keyword    : pick(d, 'end_Keyword', 'CORP'),
					matchType  : pick(d, 'end_MatchType', 'End With'),
					awb        : pick(d, 'awb_EndWith', ''),
					consignee  : pick(d, 'consignee_EndWith', 'ARAMEX CORP'),
					expectedSbt: 'B2B'
				]
			]

			for (Map scenario : scenarios) {
				String note = "B2B-003 ${scenario.step} — keyword='${scenario.keyword}' matchType='${scenario.matchType}'"
				KeywordUtil.logInfo("${note} | consignee precondition='${scenario.consignee}'")
				uploadB2BKeywordRow(scenario.keyword as String, scenario.matchType as String)
				if ((scenario.awb as String)?.trim()) {
					verifyShipmentBusinessTypeOn360View(
						scenario.awb as String,
						scenario.expectedSbt as String,
						category,
						customsType,
						sbtFilter,
						chargeStatus
					)
				} else {
					KeywordUtil.markWarning(
						"${note}: AWB not set — keyword upload verified; ingest shipment with consignee " +
						"'${scenario.consignee}' then set awb_* in B2B_003 script."
					)
				}
				WebUI.takeScreenshot()
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeB2B003MatchTypeClassificationFlow failed: ${e.message}")
		}
	}

	private static String pick(Map data, String key, String defaultValue) {
		String v = (data[key] ?: '').toString().trim()
		return v ?: (defaultValue ?: '').trim()
	}

	private static TestObject toBoe(String name, Map binding = null) {
		if (binding) {
			return findTestObject(OR_BOE_PREFIX + name, binding)
		}
		return findTestObject(OR_BOE_PREFIX + name)
	}

	private static List<String> boeSlotLabels(String slotKey) {
		String key = (slotKey ?: '').trim().toLowerCase().replaceAll('\\s+', '')
		if (key in ['boe1', 'boe 1']) {
			return ['boe1', 'boe 1']
		}
		if (key in ['boe2', 'boe 2']) {
			return ['boe2', 'boe 2']
		}
		if (key in ['boe3', 'boe 3']) {
			return ['boe3', 'boe 3']
		}
		if (key.contains('add')) {
			return ['additional', 'additional response', 'additional resp']
		}
		return [key]
	}

	@Keyword
	def openUploadActualsScreenReady(String entity = 'RUH') {
		try {
			customSearch.navigateToStageCustomsSearchScreen()
			WebUI.takeScreenshot()
			uea.clickUploadCharge()
			WebUI.takeScreenshot()
			uea.selectTypeOfData('Actuals')
			WebUI.takeScreenshot()
			uea.openUploadActualsScreen()
			uea.verifyUploadActualsScreenControlsVisible()
			if ((entity ?: '').trim()) {
				uea.selectUploadChargesDropdownValue('entity', entity.trim())
				WebUI.takeScreenshot()
			}
		} catch (Exception e) {
			failStop("openUploadActualsScreenReady failed: ${e.message}")
		}
	}

	@Keyword
	def uploadActualsWithDeclarationNumber(String awb, String declarationNumber, String entity = 'RUH', boolean expectSuccess = true) {
		try {
			String tracking = (awb ?: '').trim()
			String decl = (declarationNumber ?: '').trim()
			if (!tracking) {
				failStop('uploadActualsWithDeclarationNumber: AWB is required (hardcode awb_* in the test script)')
			}
			if (!decl) {
				failStop('uploadActualsWithDeclarationNumber: declarationNumber is required')
			}
			String path = uea.createValidActualsExcel(tracking, [declarationNumber: decl])
			KeywordUtil.logInfo("Uploading actuals AWB='${tracking}' Declaration Number='${decl}' file='${path}'")
			openUploadActualsScreenReady(entity)
			uea.selectEntityAndUploadActuals(path, entity)
			WebUI.takeScreenshot()
			if (expectSuccess) {
				uea.verifyActualUploadSuccess()
			} else {
				verifyDuplicateDeclarationNumberRejected(decl)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadActualsWithDeclarationNumber(${awb}, ${declarationNumber}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDuplicateDeclarationNumberRejected(String declarationNumber = '') {
		try {
			TestObject dupDecl = toBoe('msg_duplicateDeclarationNumber')
			TestObject genericDup = findTestObject('Upload_Estimate_Actuals/CCIPH-59/msg_duplicateCostBooking')
			TestObject validation = findTestObject('Upload_Estimate_Actuals/CCIPH-221/msg_validationError')
			TestObject failedReport = findTestObject('Upload_Estimate_Actuals/CCIPH-58/btn_downloadFailedReport')
			boolean visible = WebUI.waitForElementVisible(dupDecl, 20, FailureHandling.OPTIONAL)
			if (!visible) {
				visible = WebUI.waitForElementVisible(genericDup, 8, FailureHandling.OPTIONAL)
			}
			if (!visible) {
				visible = WebUI.waitForElementVisible(validation, 8, FailureHandling.OPTIONAL)
			}
			if (!visible) {
				visible = WebUI.waitForElementVisible(failedReport, 8, FailureHandling.OPTIONAL)
			}
			if (!visible) {
				failStop(
					"Expected duplicate Declaration Number validation was not displayed" +
					((declarationNumber ?: '').trim() ? " for '${declarationNumber}'" : '')
				)
			}
			KeywordUtil.logInfo("Duplicate Declaration Number validation displayed for '${declarationNumber}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDuplicateDeclarationNumberRejected failed: ${e.message}")
		}
	}

	@Keyword
	def openChargeDetail360ViewForAwb(String trackingNumber) {
		try {
			String tracking = (trackingNumber ?: '').trim()
			if (!tracking) {
				failStop('openChargeDetail360ViewForAwb: trackingNumber is required')
			}
			customSearch.navigateToStageCustomsSearchScreen()
			WebUI.takeScreenshot()
			customSearch.enterTrackingNumber(tracking)
			WebUI.takeScreenshot()
			customSearch.clickSearch()
			customSearch.verifySearchResultsDisplayed()
			WebUI.takeScreenshot()
			customSearch.clickFirstSearchResultRow()
			WebUI.takeScreenshot()
			TestObject ecdTab = findTestObject('CustomSearch_360ViewScreen/CCIPH-120/tab_ECD')
			if (WebUI.waitForElementVisible(ecdTab, 10, FailureHandling.OPTIONAL)) {
				WebUI.waitForElementClickable(ecdTab, DEFAULT_TIMEOUT)
				WebUI.click(ecdTab)
				WebUI.takeScreenshot()
			}
			waitForBoeNumberSection()
		} catch (Exception e) {
			failStop("openChargeDetail360ViewForAwb(${trackingNumber}) failed: ${e.message}")
		}
	}

	@Keyword
	def waitForBoeNumberSection() {
		try {
			TestObject boeNumber = toBoe('hdr_boeNumber')
			TestObject invoicing = findTestObject('CustomSearch_360ViewScreen/CCIPH-120/tbl_invoicingHeaders')
			boolean boeVisible = WebUI.waitForElementVisible(boeNumber, DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)
			if (!boeVisible) {
				WebUI.waitForElementVisible(invoicing, DEFAULT_TIMEOUT)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("waitForBoeNumberSection failed: ${e.message}")
		}
	}

	@Keyword
	def verifyBoeSlotHeadersVisible() {
		try {
			waitForBoeNumberSection()
			TestObject boeNumber = toBoe('hdr_boeNumber')
			if (WebUI.waitForElementVisible(boeNumber, 10, FailureHandling.OPTIONAL)) {
				WebUI.verifyElementVisible(boeNumber, FailureHandling.STOP_ON_FAILURE)
			} else {
				KeywordUtil.logInfo('BOE NUMBER label not found with exact locator — verifying BOE1/BOE2/BOE3/Additional slots')
			}
			['boe1', 'boe2', 'boe3', 'additional'].each { String slot ->
				TestObject header = toBoe('hdr_boeSlot', [boeSlot: slot])
				if (!WebUI.waitForElementVisible(header, 10, FailureHandling.OPTIONAL)) {
					failStop("Expected BOE slot header '${slot}' to be visible on 360 View invoicing table")
				}
				KeywordUtil.logInfo("Verified BOE slot header visible: ${slot}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyBoeSlotHeadersVisible failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDeclarationMappedToBoeSlot(String boeSlot, String declarationNumber) {
		try {
			String slot = (boeSlot ?: '').trim()
			String decl = (declarationNumber ?: '').trim()
			if (!slot || !decl) {
				failStop('verifyDeclarationMappedToBoeSlot: boeSlot and declarationNumber are required')
			}
			boolean mapped = false
			for (String label : boeSlotLabels(slot)) {
				TestObject mappedValue = toBoe('val_boeMappedValue', [boeSlot: label, declarationNumber: decl])
				if (WebUI.waitForElementVisible(mappedValue, 8, FailureHandling.OPTIONAL)) {
					mapped = true
					KeywordUtil.logInfo("Declaration '${decl}' mapped to '${label}'")
					break
				}
				TestObject header = toBoe('hdr_boeSlot', [boeSlot: label])
				if (WebUI.waitForElementVisible(header, 5, FailureHandling.OPTIONAL)) {
					String headerText = (WebUI.getText(header, FailureHandling.OPTIONAL) ?: '').trim()
					if (headerText.contains(decl)) {
						mapped = true
						KeywordUtil.logInfo("Declaration '${decl}' found in '${label}' header text '${headerText}'")
						break
					}
				}
			}
			if (!mapped) {
				failStop("Declaration Number '${decl}' is not mapped to BOE slot '${slot}' on 360 View")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyDeclarationMappedToBoeSlot(${boeSlot}, ${declarationNumber}) failed: ${e.message}")
		}
	}

	@Keyword
	def verifyBoeDeclarationMappings(Map<String, String> slotToDeclaration) {
		try {
			verifyBoeSlotHeadersVisible()
			Map mappings = slotToDeclaration ?: [:]
			mappings.each { Object slot, Object decl ->
				verifyDeclarationMappedToBoeSlot(slot?.toString(), decl?.toString())
			}
			mappings.each { Object slot, Object decl ->
				mappings.each { Object otherSlot, Object otherDecl ->
					if (slot?.toString() == otherSlot?.toString()) {
						return
					}
					String other = (otherDecl?.toString() ?: '').trim()
					if (!other) {
						return
					}
					for (String label : boeSlotLabels(slot?.toString())) {
						TestObject swapped = toBoe('val_boeMappedValue', [boeSlot: label, declarationNumber: other])
						if (WebUI.waitForElementVisible(swapped, 3, FailureHandling.OPTIONAL)) {
							failStop("Declaration '${other}' is mapped to '${slot}' (expected on '${otherSlot}'); values appear swapped")
						}
					}
				}
			}
			KeywordUtil.logInfo("Verified BOE Declaration Number mappings: ${mappings}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyBoeDeclarationMappings failed: ${e.message}")
		}
	}

	@Keyword
	def executeBoe001FourSlotMappingFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String awb = pick(d, 'awb_Primary', '')
			String entity = pick(d, 'entity_Name', 'RUH')
			Map<String, String> mappings = [
				'boe1'       : pick(d, 'decl_BOE1', 'DEC-B1-001'),
				'boe2'       : pick(d, 'decl_BOE2', 'DEC-B2-002'),
				'boe3'       : pick(d, 'decl_BOE3', 'DEC-B3-003'),
				'additional' : pick(d, 'decl_Additional', 'DEC-ADD-004')
			]
			if (!awb) {
				KeywordUtil.markWarning(
					'BOE_001: awb_Primary is blank — skip upload/360 mapping. Hardcode AWB in Scripts/Feedback_PatchRelease/CCIPH-549/BOE_001 after a test shipment exists.'
				)
				WebUI.takeScreenshot()
				return
			}
			mappings.each { String slot, String decl ->
				KeywordUtil.logInfo("BOE_001 upload for ${slot} Declaration Number='${decl}'")
				uploadActualsWithDeclarationNumber(awb, decl, entity, true)
			}
			openChargeDetail360ViewForAwb(awb)
			verifyBoeDeclarationMappings(mappings)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeBoe001FourSlotMappingFlow failed: ${e.message}")
		}
	}

	@Keyword
	def executeBoe002RetainRespectiveNumbersFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String awb = pick(d, 'awb_Primary', '')
			String entity = pick(d, 'entity_Name', 'RUH')
			Map<String, String> mappings = [
				'boe1': pick(d, 'decl_BOE1', 'DEC-B1-001'),
				'boe2': pick(d, 'decl_BOE2', 'DEC-B2-002'),
				'boe3': pick(d, 'decl_BOE3', 'DEC-B3-003')
			]
			if (!awb) {
				KeywordUtil.markWarning(
					'BOE_002: awb_Primary is blank — skip upload/360 mapping. Hardcode AWB in Scripts/Feedback_PatchRelease/CCIPH-549/BOE_002.'
				)
				WebUI.takeScreenshot()
				return
			}
			mappings.each { String slot, String decl ->
				uploadActualsWithDeclarationNumber(awb, decl, entity, true)
			}
			openChargeDetail360ViewForAwb(awb)
			verifyBoeDeclarationMappings(mappings)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeBoe002RetainRespectiveNumbersFlow failed: ${e.message}")
		}
	}

	@Keyword
	def executeBoe003SameShipmentDuplicateRejectedFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String awb = pick(d, 'awb_Primary', '')
			String entity = pick(d, 'entity_Name', 'RUH')
			String decl = pick(d, 'decl_Duplicate', 'DEC001')
			if (!awb) {
				KeywordUtil.markWarning(
					'BOE_003: awb_Primary is blank — skip duplicate Declaration Number check. Hardcode AWB in Scripts/Feedback_PatchRelease/CCIPH-549/BOE_003.'
				)
				WebUI.takeScreenshot()
				return
			}
			uploadActualsWithDeclarationNumber(awb, decl, entity, true)
			uploadActualsWithDeclarationNumber(awb, decl, entity, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeBoe003SameShipmentDuplicateRejectedFlow failed: ${e.message}")
		}
	}

	@Keyword
	def executeBoe004CrossShipmentDuplicateRejectedFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String awbA = pick(d, 'awb_ShipmentA', '')
			String awbB = pick(d, 'awb_ShipmentB', '')
			String entity = pick(d, 'entity_Name', 'RUH')
			String decl = pick(d, 'decl_Duplicate', 'DEC001')
			if (!awbA || !awbB) {
				KeywordUtil.markWarning(
					'BOE_004: awb_ShipmentA/awb_ShipmentB blank — skip cross-shipment duplicate check. Hardcode AWBs in Scripts/Feedback_PatchRelease/CCIPH-549/BOE_004.'
				)
				WebUI.takeScreenshot()
				return
			}
			uploadActualsWithDeclarationNumber(awbA, decl, entity, true)
			uploadActualsWithDeclarationNumber(awbB, decl, entity, false)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeBoe004CrossShipmentDuplicateRejectedFlow failed: ${e.message}")
		}
	}

	@Keyword
	def executeBoe005UniqueDeclarationsAcrossShipmentsFlow(Map<String, String> data = [:]) {
		try {
			Map d = data ?: [:]
			String awbA = pick(d, 'awb_ShipmentA', '')
			String awbB = pick(d, 'awb_ShipmentB', '')
			String entity = pick(d, 'entity_Name', 'RUH')
			String declA = pick(d, 'decl_ShipmentA', 'DEC001')
			String declB = pick(d, 'decl_ShipmentB', 'DEC002')
			if (!awbA || !awbB) {
				KeywordUtil.markWarning(
					'BOE_005: awb_ShipmentA/awb_ShipmentB blank — skip unique mapping. Hardcode AWBs in Scripts/Feedback_PatchRelease/CCIPH-549/BOE_005.'
				)
				WebUI.takeScreenshot()
				return
			}
			uploadActualsWithDeclarationNumber(awbA, declA, entity, true)
			uploadActualsWithDeclarationNumber(awbB, declB, entity, true)
			openChargeDetail360ViewForAwb(awbA)
			verifyDeclarationMappedToBoeSlot('boe1', declA)
			openChargeDetail360ViewForAwb(awbB)
			verifyDeclarationMappedToBoeSlot('boe1', declB)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeBoe005UniqueDeclarationsAcrossShipmentsFlow failed: ${e.message}")
		}
	}

	@Keyword
	def verifyB2BKeywordsManagerAnchorVisible() {
		try {
			WebUI.waitForElementVisible(findTestObject(OR_B2B_PREFIX + 'txt_pageHeading_B2BKeywordsManager'), DEFAULT_TIMEOUT)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyB2BKeywordsManagerAnchorVisible failed: ${e.message}")
		}
	}
}
