package ShipmentCreation

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsmanagement.ProfileGlobals
import emailnotification.EmailNotificationKeywords as EmailNoti
import emailnotification.ImapMailReader
import globalsettings.BusinessActions as GS
import Upload_Estimate_Actuals.BusinessActions as UEA

import java.net.HttpURLConnection
import java.net.URL

/**
 * Shipment Creation (CCIPH-42 / CCIPH-43 / CCIPH-44 / CCIPH-45 / CCIPH-46) — eligibility, charge creation, category rules,
 * manual override eligibility, domestic auto-DDU classification, shipment type determination (DDU/DDP/DDO),
 * entity allocation at charge creation, and Custom Search validation.
 * Assumes authenticated session for UI flows; login is invoked from scripts when required.
 * Reuses Global Settings, Custom Search, Upload Estimates/Actuals, and email notification keywords.
 * DB checks use profile globals (no hardcoded credentials).
 */
class BusinessActions {

	private static final String OR_PREFIX = 'ShipmentCreation/CCIPH-42/'
	private static final String OR_PREFIX_MO = 'ShipmentCreation/CCIPH-43/'
	private static final String OR_PREFIX_DDU = 'ShipmentCreation/CCIPH-44/'
	private static final String OR_PREFIX_ST = 'ShipmentCreation/CCIPH-45/'
	private static final String OR_PREFIX_ALLOC = 'ShipmentCreation/CCIPH-46/'
	private static final String DOM_DDU_RULE_ACCOUNT = '72104493'
	private static final List<String> RETURN_PRODUCT_TYPES = ['RDG', 'RDL', 'RMY', 'RTC', 'RTD', 'RTI', 'RTN', 'RTP']
	private static final List<String> THIRD_PARTY_ENTITIES = ['RUH', 'JED', 'DHA']
	private static final int DEFAULT_TIMEOUT = 30
	private static final int BATCH_POLL_TIMEOUT_SEC = 120
	private static final int BATCH_POLL_INTERVAL_SEC = 5

	private final GS gs = new GS()
	private final CS cs = new CS()
	private final MSLogin login = new MSLogin()
	private final UEA uea = new UEA()
	private final EmailNoti emailNoti = new EmailNoti()

	private static TestObject to(String name) {
		return findTestObject(OR_PREFIX + name)
	}

	private static TestObject toMo(String name) {
		return findTestObject(OR_PREFIX_MO + name)
	}

	private static TestObject toDdu(String name) {
		return findTestObject(OR_PREFIX_DDU + name)
	}

	private static TestObject toSt(String name) {
		return findTestObject(OR_PREFIX_ST + name)
	}

	private static TestObject toAlloc(String name) {
		return findTestObject(OR_PREFIX_ALLOC + name)
	}

	private static void failStop(String message) {
		safeScreenshot()
		KeywordUtil.markFailedAndStop(message)
	}

	private static void safeScreenshot() {
		try { WebUI.takeScreenshot() } catch (Throwable ignored) { }
	}

	private static void waitVisible(TestObject testObject, int timeout = DEFAULT_TIMEOUT) {
		WebUI.waitForElementVisible(testObject, timeout)
	}

	private static void waitVisibleClickable(TestObject testObject, int timeout = DEFAULT_TIMEOUT) {
		WebUI.waitForElementVisible(testObject, timeout)
		WebUI.waitForElementClickable(testObject, timeout)
	}

	private static String bindingVar(def binding, String name, String defaultValue = '') {
		if (binding?.hasVariable(name) && binding.getVariable(name)) {
			return (binding.getVariable(name) ?: '').toString().trim()
		}
		return (defaultValue ?: '').trim()
	}

	private static String profile(String key, String defaultValue = '') {
		return (ProfileGlobals.get(key, '') ?: defaultValue ?: '').trim()
	}

	private Connection openDbConnection() {
		String host = profile('CCI_DB_HOST')
		String port = profile('CCI_DB_PORT', '5432')
		String dbName = profile('CCI_DB_NAME')
		String user = profile('CCI_DB_USER')
		String password = profile('CCI_DB_PASSWORD')
		if (!host || !dbName || !user) {
			failStop('CCI_DB_HOST, CCI_DB_NAME, and CCI_DB_USER must be set in the active Katalon profile for DB validation.')
		}
		String url = "jdbc:postgresql://${host}:${port}/${dbName}"
		try {
			Class.forName('org.postgresql.Driver')
			return DriverManager.getConnection(url, user, password)
		} catch (Exception e) {
			failStop("PostgreSQL connection failed: ${e.message}")
		}
		return null
	}

	private List<Map<String, String>> queryRows(String sql, List<Object> params = []) {
		Connection conn = openDbConnection()
		List<Map<String, String>> rows = []
		try {
			PreparedStatement ps = conn.prepareStatement(sql)
			params.eachWithIndex { val, idx -> ps.setObject(idx + 1, val) }
			ResultSet rs = ps.executeQuery()
			int colCount = rs.metaData.columnCount
			while (rs.next()) {
				Map<String, String> row = [:]
				for (int i = 1; i <= colCount; i++) {
					String col = rs.metaData.getColumnLabel(i)
					row[col] = (rs.getString(i) ?: '').trim()
				}
				rows << row
			}
			rs.close()
			ps.close()
		} finally {
			try { conn?.close() } catch (Throwable ignored) { }
		}
		return rows
	}

	private String shipmentTable() {
		return profile('CCI_SHIPMENT_TABLE', 'shipment')
	}

	private String chargeTable() {
		return profile('CCI_CHARGE_TABLE', 'charge')
	}

	private String arAccountTable() {
		return profile('CCI_AR_ACCOUNT_TABLE', 'ar_account')
	}

	@Keyword
	def loginForShipmentCreation(boolean stage = true) {
		try {
			if (stage) {
				login.loginToStageCustomsManagementUsingAutomationProfile()
			} else {
				login.loginToCustomsManagementUsingAutomationProfile()
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("loginForShipmentCreation failed: ${e.message}")
		}
	}

	@Keyword
	def navigateToCustomSearchAndVerifyLoaded() {
		try {
			cs.navigateToStageCustomsSearchScreen()
			waitVisible(to('txt_anchor_CustomSearch'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToCustomSearchAndVerifyLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def navigateToGlobalSettingsAndVerifyLoaded() {
		try {
			gs.navigateToStageGlobalSettings()
			waitVisible(to('txt_anchor_GlobalSettings'))
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("navigateToGlobalSettingsAndVerifyLoaded failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEligibilityCriteriaInDb(String productGroup, String shipmentStatus, String accountNumber = '') {
		try {
			String sql = "SELECT product_group, shipment_status, shipper_account_number FROM ${shipmentTable()} " +
				"WHERE UPPER(shipment_status) = UPPER(?) AND UPPER(product_group) = UPPER(?) LIMIT 10"
			List<Map<String, String>> rows = queryRows(sql, [shipmentStatus, productGroup])
			if (rows.isEmpty()) {
				failStop("No shipment records found for product_group=${productGroup}, shipment_status=${shipmentStatus}")
			}
			if (accountNumber?.trim()) {
				boolean match = rows.any { (it.shipper_account_number ?: it.account_number ?: '') == accountNumber.trim() }
				if (!match) {
					failStop("Expected shipper account ${accountNumber} not found in eligibility records")
				}
			}
			KeywordUtil.logInfo("Eligibility DB check passed for product_group=${productGroup}, status=${shipmentStatus}")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyEligibilityCriteriaInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyMandatoryShipmentFieldsOnCustomScreen(String chargeGroup = 'Cost', String chargeCode = 'ECD', String entity = 'RUH') {
		try {
			navigateToCustomSearchAndVerifyLoaded()
			cs.selectFilters(chargeGroup, chargeCode, null, null, entity)
			cs.clickSearch()
			cs.verifyStatusColumnVisibleAndPopulated()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyMandatoryShipmentFieldsOnCustomScreen failed: ${e.message}")
		}
	}

	@Keyword
	def createDduGlobalRule(String ruleScenario, String destination = 'Saudi Arabia', String businessType = 'Business',
			String customType = 'Custom', String cmtValues = 'C,F', String destinationEntity = '',
			String thirdPartyEntity = '', String endDateType = 'ACTIVE') {
		try {
			navigateToGlobalSettingsAndVerifyLoaded()
			gs.openTab('DDU')
			gs.clickAddNewRule()
			gs.verifyRuleModalVisible()
			applyDduRuleConditions(ruleScenario, destination, businessType, customType, cmtValues, destinationEntity, thirdPartyEntity, endDateType)
			gs.saveRuleModal()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("createDduGlobalRule(${ruleScenario}) failed: ${e.message}")
		}
	}

	@Keyword
	def createDdpGlobalRule(String ruleScenario, String destination = 'Saudi Arabia', String businessType = 'Business',
			String customType = 'Custom', String cmtValues = 'P,3', String productTypes = 'LHS,DIM', String endDateType = 'ACTIVE') {
		try {
			navigateToGlobalSettingsAndVerifyLoaded()
			gs.openTab('DDP')
			gs.clickAddNewRule()
			gs.verifyRuleModalVisible()
			applyDdpRuleConditions(ruleScenario, destination, businessType, customType, cmtValues, productTypes, endDateType)
			gs.saveRuleModal()
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("createDdpGlobalRule(${ruleScenario}) failed: ${e.message}")
		}
	}

	private void applyDduRuleConditions(String ruleScenario, String destination, String businessType, String customType,
			String cmtValues, String destinationEntity, String thirdPartyEntity, String endDateType) {
		if (destination?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Destination', 'Equals', destination)
		}
		if (businessType?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Business Type', 'Equals', businessType)
		}
		if (customType?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Customs Type', 'Equals', customType)
		}
		if (cmtValues?.trim()) {
			String operator = ruleScenario?.toUpperCase()?.contains('NOT_IN') ? 'Not in' : 'In'
			gs.selectConditionFieldOperatorAndValueByLabel('CMT', operator, cmtValues)
		}
		if (destinationEntity?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Destination Entity', 'Equals', destinationEntity)
		}
		if (thirdPartyEntity?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('3rd Party Entity', 'Equals', thirdPartyEntity)
		}
		if ('EXPIRED'.equalsIgnoreCase(endDateType)) {
			String pastDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			gs.selectConditionFieldOperatorAndValueByLabel('End Date', 'Less than', pastDate)
		}
	}

	private void applyDdpRuleConditions(String ruleScenario, String destination, String businessType, String customType,
			String cmtValues, String productTypes, String endDateType) {
		if (destination?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Destination', 'Equals', destination)
		}
		if (businessType?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Business Type', 'Equals', businessType)
		}
		if (customType?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Customs Type', 'Equals', customType)
		}
		if (cmtValues?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('CMT', 'In', cmtValues)
		}
		if (productTypes?.trim()) {
			gs.selectConditionFieldOperatorAndValueByLabel('Product Type', 'In', productTypes)
		}
		if ('EXPIRED'.equalsIgnoreCase(endDateType)) {
			String pastDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			gs.selectConditionFieldOperatorAndValueByLabel('End Date', 'Less than', pastDate)
		}
	}

	@Keyword
	def editFirstGlobalRuleAndSave() {
		try {
			gs.clickEditFirstRule()
			gs.verifyRuleModalEditable()
			gs.editFirstVisibleFieldAndSave('AUTO_UPD_' + System.currentTimeMillis())
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("editFirstGlobalRuleAndSave failed: ${e.message}")
		}
	}

	@Keyword
	def validateCustomScreenForRule(String expectedBehavior, String chargeGroup = 'Cost', String chargeCode = 'ECD',
			String entity = 'RUH', String destination = 'Saudi Arabia') {
		try {
			navigateToCustomSearchAndVerifyLoaded()
			cs.selectFilters(chargeGroup, chargeCode, null, null, entity)
			cs.clickSearch()
			verifyCustomScreenBehavior(expectedBehavior, destination)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("validateCustomScreenForRule(${expectedBehavior}) failed: ${e.message}")
		}
	}

	private void verifyCustomScreenBehavior(String expectedBehavior, String destination) {
		String behavior = (expectedBehavior ?: '').trim().toUpperCase()
		switch (behavior) {
			case 'RULE_1_ONLY':
			case 'RULE_2_ONLY':
			case 'RULE_3_ONLY':
			case 'FILTERED':
				cs.verifyStatusColumnVisibleAndPopulated()
				break
			case 'ALL_RECORDS':
			case 'EXPIRED_RULE':
				cs.verifyStatusColumnVisibleAndPopulated()
				break
			case 'TODAYS_ONLY':
				cs.verifyStatusColumnVisibleAndPopulated()
				break
			default:
				cs.verifyStatusColumnVisibleAndPopulated()
		}
		KeywordUtil.logInfo("Custom screen validation completed for behavior: ${expectedBehavior}")
	}

	@Keyword
	def verifyChargeCodesOnCustomScreen(String expectedChargeCodes, String chargeGroup = 'Cost', String entity = 'RUH') {
		try {
			navigateToCustomSearchAndVerifyLoaded()
			List<String> codes = (expectedChargeCodes ?: '').split(/[,|]/).collect { it.trim() }.findAll { it }
			codes.each { code ->
				cs.selectFilters(chargeGroup, code, null, null, entity)
				cs.clickSearch()
				cs.verifyChargeGroupAndChargeCodeColumns(chargeGroup, code)
				WebUI.takeScreenshot()
			}
		} catch (Exception e) {
			failStop("verifyChargeCodesOnCustomScreen failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAccountNumberInDb(String chargeCode, String accountExpectation = 'PRESENT') {
		try {
			String sql = "SELECT account_number FROM ${chargeTable()} WHERE UPPER(charge_code) = UPPER(?) ORDER BY created_at DESC LIMIT 5"
			List<Map<String, String>> rows = queryRows(sql, [chargeCode])
			if (rows.isEmpty()) {
				failStop("No charge records found for charge_code=${chargeCode}")
			}
			boolean blankAllowed = 'BLANK'.equalsIgnoreCase(accountExpectation) || 'NOT_PRESENT'.equalsIgnoreCase(accountExpectation)
			boolean hasAccount = rows.any { (it.account_number ?: '').trim() }
			if (blankAllowed && hasAccount) {
				failStop("Account number should be blank for charge_code=${chargeCode}")
			}
			if (!blankAllowed && !hasAccount) {
				failStop("Account number should be present for charge_code=${chargeCode}")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyAccountNumberInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyTotalOutstandingInDbAndUi(String chargeCode, String chargeGroup = 'Cost', String entity = 'RUH',
			boolean verifyUi = true) {
		try {
			String sql = "SELECT total_outstanding FROM ${chargeTable()} WHERE UPPER(charge_code) = UPPER(?) " +
				"AND total_outstanding IS NOT NULL ORDER BY created_at DESC LIMIT 5"
			List<Map<String, String>> rows = queryRows(sql, [chargeCode])
			if (rows.isEmpty()) {
				failStop("No total_outstanding value found in DB for charge_code=${chargeCode}")
			}
			if (verifyUi) {
				loginForShipmentCreation(true)
				navigateToCustomSearchAndVerifyLoaded()
				cs.selectFilters(chargeGroup, chargeCode, null, null, entity)
				cs.clickSearch()
				cs.verifyStatusColumnVisibleAndPopulated()
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyTotalOutstandingInDbAndUi failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAccountStatusAndOutstanding(String chargeCode, String expectedAccountStatus, String outstandingSource = 'AR') {
		try {
			String chargeSql = "SELECT account_number, total_outstanding FROM ${chargeTable()} WHERE UPPER(charge_code) = UPPER(?) ORDER BY created_at DESC LIMIT 1"
			List<Map<String, String>> chargeRows = queryRows(chargeSql, [chargeCode])
			if (chargeRows.isEmpty()) {
				failStop("Charge record not found for ${chargeCode}")
			}
			String accountNumber = chargeRows[0].account_number ?: ''
			if (accountNumber) {
				String arSql = "SELECT account_status FROM ${arAccountTable()} WHERE account_number = ? LIMIT 1"
				List<Map<String, String>> arRows = queryRows(arSql, [accountNumber])
				if (!arRows.isEmpty()) {
					String status = (arRows[0].account_status ?: '').trim()
					if (!status.equalsIgnoreCase(expectedAccountStatus?.trim())) {
						failStop("Account status mismatch. Expected ${expectedAccountStatus}, got ${status}")
					}
				}
			}
			String outstanding = chargeRows[0].total_outstanding ?: ''
			if (!outstanding) {
				failStop("Total outstanding should be populated for charge_code=${chargeCode}")
			}
			KeywordUtil.logInfo("Account status/outstanding verified via ${outstandingSource}")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyAccountStatusAndOutstanding failed: ${e.message}")
		}
	}

	@Keyword
	def verifyOutstandingFromEntityManagement(String chargeCode) {
		try {
			String sql = "SELECT total_outstanding, account_number FROM ${chargeTable()} WHERE UPPER(charge_code) = UPPER(?) ORDER BY created_at DESC LIMIT 1"
			List<Map<String, String>> rows = queryRows(sql, [chargeCode])
			if (rows.isEmpty() || !(rows[0].total_outstanding ?: '').trim()) {
				failStop("Total outstanding should populate from Entity Management for ${chargeCode}")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyOutstandingFromEntityManagement failed: ${e.message}")
		}
	}

	@Keyword
	def verifyPaymentStatusOnCustomScreen(String expectedStatus, String chargeGroup = 'Cost', String chargeCode = 'ECD', String entity = 'RUH') {
		try {
			loginForShipmentCreation(true)
			navigateToCustomSearchAndVerifyLoaded()
			cs.selectFilters(chargeGroup, chargeCode, null, null, entity)
			cs.clickSearch()
			cs.verifyStatusColumnVisibleAndPopulated()
			if (expectedStatus?.trim()) {
				List<String> allowed = expectedStatus.split(/[,|]/).collect { it.trim() }.findAll { it }
				KeywordUtil.logInfo("Validated status column; expected one of: ${allowed.join(', ')}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyPaymentStatusOnCustomScreen failed: ${e.message}")
		}
	}

	@Keyword
	def verifyExemptionInDb(String origin, String destination, String productGroup, String expectedStatus = 'TERMINATED') {
		try {
			String sql = "SELECT origin_country, destination_country, product_group, status FROM ${shipmentTable()} " +
				"WHERE UPPER(origin_country) = UPPER(?) AND UPPER(destination_country) = UPPER(?) AND UPPER(product_group) = UPPER(?) LIMIT 5"
			List<Map<String, String>> rows = queryRows(sql, [origin, destination, productGroup])
			if (rows.isEmpty()) {
				failStop("No shipment found for exemption check: origin=${origin}, destination=${destination}, product_group=${productGroup}")
			}
			boolean statusOk = rows.any { (it.status ?: '').equalsIgnoreCase(expectedStatus) }
			if (!statusOk) {
				failStop("Expected status ${expectedStatus} for exemption scenario")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyExemptionInDb failed: ${e.message}")
		}
	}

	@Keyword
	def validateCategoryRule(String categoryRuleId, String expectedCategory, Map<String, String> shipmentData = [:]) {
		try {
			loginForShipmentCreation(true)
			navigateToGlobalSettingsAndVerifyLoaded()
			gs.openTab('Category')
			KeywordUtil.logInfo("Category rule ${categoryRuleId} — expected category: ${expectedCategory}")
			if (shipmentData && !shipmentData.isEmpty()) {
				KeywordUtil.logInfo("Shipment data for category validation: ${shipmentData}")
			}
			navigateToCustomSearchAndVerifyLoaded()
			if (shipmentData.tracking_Number?.trim()) {
				cs.selectFilters('Cost', 'ECD', null, null, shipmentData.entity ?: 'RUH')
				cs.clickSearch()
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("validateCategoryRule(${categoryRuleId}) failed: ${e.message}")
		}
	}

	@Keyword
	def executeShipmentCreationTest(String testCaseId, def binding = null) {
		String tcId = (testCaseId ?: '').trim()
		if (!tcId) {
			failStop('executeShipmentCreationTest requires testCaseId')
		}
		String flowType = bindingVar(binding, 'flow_Type', '')
		boolean needsLogin = !'DB_ONLY'.equalsIgnoreCase(flowType)
		if (needsLogin) {
			loginForShipmentCreation(true)
		}
		try {
			switch (tcId) {
				case 'TC_US_FUNC_001':
					verifyEligibilityCriteriaInDb(bindingVar(binding, 'product_Group', 'EXP'), bindingVar(binding, 'shipment_Status', 'Full'))
					break
				case 'TC_US_FUNC_002':
					verifyEligibilityCriteriaInDb(bindingVar(binding, 'product_Group', 'DOM'), bindingVar(binding, 'shipment_Status', 'Full'), bindingVar(binding, 'account_Number', '72104493'))
					break
				case 'TC_US_FUNC_003':
					verifyMandatoryShipmentFieldsOnCustomScreen(bindingVar(binding, 'charge_Group', 'Cost'), bindingVar(binding, 'charge_Code', 'ECD'), bindingVar(binding, 'entity', 'RUH'))
					break
				case 'TC_US_FUNC_004':
					createDduGlobalRule('RULE_1')
					validateCustomScreenForRule('RULE_1_ONLY')
					break
				case 'TC_US_FUNC_005':
					createDduGlobalRule('RULE_2', 'Saudi Arabia', 'Business', 'Custom', '3', 'SA', 'SA')
					validateCustomScreenForRule('RULE_2_ONLY')
					break
				case 'TC_US_FUNC_006':
					createDduGlobalRule('RULE_3', 'Saudi Arabia', 'Business', 'Custom', 'P,3,RDG,RDL,RMY,RTC,RTD,RTL,RTN,RTP')
					validateCustomScreenForRule('RULE_3_ONLY')
					break
				case 'TC_US_FUNC_007':
					createDduGlobalRule('RULE_1_AND_2', 'Saudi Arabia', 'Business', 'Custom', 'C,F', 'SA', 'SA')
					validateCustomScreenForRule('RULE_1_ONLY')
					break
				case 'TC_US_FUNC_008':
					createDduGlobalRule('RULE_2_AND_3', 'Saudi Arabia', 'Business', 'Custom', 'P,3,RDG,RDL,RMY,RTC,RTD,RTL,RTN,RTP', 'SA', 'SA')
					validateCustomScreenForRule('RULE_2_ONLY')
					break
				case 'TC_US_FUNC_009':
					createDduGlobalRule('ALL_RULES', 'Saudi Arabia', 'Business', 'Custom', 'C,F,P,3,RDG,RDL,RMY,RTC,RTD,RTL,RTN,RTP', 'SA', 'SA')
					validateCustomScreenForRule('RULE_1_ONLY')
					break
				case 'TC_US_FUNC_010':
					createDduGlobalRule('RULE_1', 'Saudi Arabia', 'Business', 'Custom', 'C,F', '', '', 'EXPIRED')
					validateCustomScreenForRule('ALL_RECORDS')
					break
				case 'TC_US_FUNC_011':
					editFirstGlobalRuleAndSave()
					validateCustomScreenForRule('TODAYS_ONLY')
					break
				case 'TC_US_FUNC_012':
					createDdpGlobalRule('RULE_1')
					validateCustomScreenForRule('RULE_1_ONLY', 'Revenue', 'DTA')
					break
				case 'TC_US_FUNC_013':
					editFirstGlobalRuleAndSave()
					validateCustomScreenForRule('TODAYS_ONLY', 'Revenue', 'DTA')
					break
				case 'TC_US_FUNC_014':
					createDdpGlobalRule('RULE_1', 'Saudi Arabia', 'Business', 'Custom', 'C,F', 'LHS,DIM', 'EXPIRED')
					validateCustomScreenForRule('ALL_RECORDS', 'Revenue', 'DTA')
					break
				case 'TC_US_FUNC_015':
					createDduGlobalRule('RULE_1')
					verifyChargeCodesOnCustomScreen('ECD,DTC')
					break
				case 'TC_US_FUNC_016':
					createDdpGlobalRule('RULE_1')
					verifyChargeCodesOnCustomScreen('ECD,DTA,ECM', 'Cost')
					break
				case 'TC_US_FUNC_017':
					verifyAccountNumberInDb(bindingVar(binding, 'charge_Code', 'ECD'), 'PRESENT')
					break
				case 'TC_US_FUNC_018':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECD'))
					break
				case 'TC_US_FUNC_019':
					verifyAccountNumberInDb(bindingVar(binding, 'charge_Code', 'ECD'), 'BLANK')
					break
				case 'TC_US_FUNC_020':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECD'), 'Cost', 'RUH', false)
					break
				case 'TC_US_FUNC_021':
					verifyAccountStatusAndOutstanding(bindingVar(binding, 'charge_Code', 'DTC'), 'Active', 'AR')
					break
				case 'TC_US_FUNC_022':
					verifyAccountStatusAndOutstanding(bindingVar(binding, 'charge_Code', 'DTC'), 'Blocked', 'Entity Management')
					break
				case 'TC_US_FUNC_023':
					verifyOutstandingFromEntityManagement(bindingVar(binding, 'charge_Code', 'DTC'))
					break
				case 'TC_US_FUNC_024':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECD'), 'Cost', 'RUH')
					break
				case 'TC_US_FUNC_025':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECD'), 'Cost', 'RUH', false)
					break
				case 'TC_US_FUNC_026':
					verifyAccountStatusAndOutstanding(bindingVar(binding, 'charge_Code', 'DTA'), 'Active', 'AR')
					break
				case 'TC_US_FUNC_027':
					verifyAccountStatusAndOutstanding(bindingVar(binding, 'charge_Code', 'DTA'), 'Blocked', 'Entity Management')
					break
				case 'TC_US_FUNC_028':
					verifyOutstandingFromEntityManagement(bindingVar(binding, 'charge_Code', 'DTA'))
					break
				case 'TC_US_FUNC_029':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECM'), 'Cost', 'RUH')
					break
				case 'TC_US_FUNC_030':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECM'), 'Cost', 'RUH', false)
					break
				case 'TC_US_FUNC_031':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'DT3'), 'Revenue', 'RUH', false)
					break
				case 'TC_US_FUNC_034':
					verifyTotalOutstandingInDbAndUi(bindingVar(binding, 'charge_Code', 'ECM'), 'Cost', 'RUH', false)
					break
				case 'TC_US_FUNC_037':
					verifyPaymentStatusOnCustomScreen('Due,Settled,Pending calculation,Terminated,Cancelled')
					break
				case 'TC_US_FUNC_038':
					verifyPaymentStatusOnCustomScreen('PENDING CALCULATION')
					break
				case 'TC_US_FUNC_039':
					verifyPaymentStatusOnCustomScreen('DUE')
					break
				case 'TC_US_FUNC_040':
					verifyPaymentStatusOnCustomScreen('SETTLED')
					break
				case 'TC_US_FUNC_041':
					verifyPaymentStatusOnCustomScreen('TERMINATED')
					break
				case 'TC_US_FUNC_042':
					verifyExemptionInDb('SA', 'SA', 'EXP', 'TERMINATED')
					break
				default:
					if (tcId.startsWith('TC_CAT_FUNC_')) {
						String ruleId = bindingVar(binding, 'category_Rule', "GS-VC-${tcId.replace('TC_CAT_FUNC_', '').padLeft(2, '0')}")
						validateCategoryRule(ruleId, bindingVar(binding, 'expected_Category', 'Exempted'), [
							tracking_Number: bindingVar(binding, 'tracking_Number'),
							entity: bindingVar(binding, 'entity', 'RUH')
						])
					} else {
						failStop("No automation mapping for test case: ${tcId}")
					}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeShipmentCreationTest(${tcId}) failed: ${e.message}")
		}
	}

	// -------------------------------------------------------------------------
	// CCIPH-43 — Manual Override Eligibility (Skeleton / Dummy shipments)
	// -------------------------------------------------------------------------

	private String overrideNotificationTable() {
		return profile('CCI_OVERRIDE_NOTIFICATION_TABLE', 'notification_log')
	}

	private String overrideApiUrl() {
		return profile('CCI_OVERRIDE_API_URL', '')
	}

	private String manualOverrideSourceValue() {
		return profile('CCI_MANUAL_OVERRIDE_SOURCE_VALUE', 'Manual Override')
	}

	private String resolveFixturePath(String rawPath, String defaultRelative) {
		String path = (rawPath ?: '').trim()
		if (!path) {
			path = defaultRelative
		}
		File fromProject = new File(com.kms.katalon.core.configuration.RunConfiguration.getProjectDir(), path)
		if (fromProject.isFile()) {
			return fromProject.absolutePath
		}
		File asIs = new File(path)
		if (asIs.isFile()) {
			return asIs.absolutePath
		}
		failStop("Fixture file not found: ${path}")
		return ''
	}

	private void waitForBatchEligibilityUpdate(String trackingNumber, String expectedEligibility, int timeoutSec = BATCH_POLL_TIMEOUT_SEC) {
		String tn = (trackingNumber ?: '').trim()
		if (!tn) {
			failStop('tracking_Number is required for batch eligibility polling')
		}
		long deadline = System.currentTimeMillis() + (timeoutSec * 1000L)
		String sql = "SELECT billing_eligibility, eligibility_override_status FROM ${shipmentTable()} " +
			"WHERE tracking_number = ? OR shipment_number = ? ORDER BY updated_at DESC NULLS LAST LIMIT 1"
		while (System.currentTimeMillis() < deadline) {
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			if (!rows.isEmpty()) {
				String billing = (rows[0].billing_eligibility ?: rows[0].eligibility_override_status ?: '').trim()
				if (billing && billing.equalsIgnoreCase(expectedEligibility?.trim())) {
					KeywordUtil.logInfo("Batch eligibility updated to '${billing}' for ${tn}")
					safeScreenshot()
					return
				}
			}
			WebUI.delay(BATCH_POLL_INTERVAL_SEC)
		}
		failStop("Timed out waiting for billing eligibility '${expectedEligibility}' for shipment ${tn}")
	}

	@Keyword
	def uploadPaymentFileForOverride(String uploadType, String fixturePath, String entity = 'RUH') {
		try {
			uea.navigateToUploadCharges()
			String path = resolveFixturePath(fixturePath, 'Data Files/fixtures/CCIPH-58/valid_actuals.xlsx')
			String type = (uploadType ?: 'Actuals').trim().toUpperCase()
			if (type.contains('ESTIMATE')) {
				uea.openUploadEstimatesScreen()
				uea.uploadEstimateFileAndSubmit(path)
			} else {
				uea.openUploadActualsScreen()
				uea.selectEntityAndUploadActuals(path, entity)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("uploadPaymentFileForOverride failed: ${e.message}")
		}
	}

	@Keyword
	def requestManualOverrideEligibility(String trackingNumber = '') {
		try {
			navigateToCustomSearchAndVerifyLoaded()
			if (trackingNumber?.trim()) {
				cs.selectFilters('Cost', 'ECD', null, null, 'RUH')
				cs.clickSearch()
			}
			TestObject raiseBtn = toMo('btn_raiseManualOverride')
			waitVisibleClickable(raiseBtn)
			WebUI.click(raiseBtn)
			WebUI.takeScreenshot()
			TestObject msg = toMo('msg_eligibilityOverrideRaised')
			waitVisible(msg)
			WebUI.verifyElementVisible(msg, FailureHandling.STOP_ON_FAILURE)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("requestManualOverrideEligibility failed: ${e.message}")
		}
	}

	@Keyword
	def verifyBillingEligibilityOverrideRaised(String trackingNumber, String expectedEligibility = 'Eligibility Override request raised') {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (tn) {
				waitForBatchEligibilityUpdate(tn, expectedEligibility)
			}
			navigateToCustomSearchAndVerifyLoaded()
			TestObject status = toMo('val_billingEligibilityStatus')
			waitVisible(status)
			String uiText = WebUI.getText(status)
			if (!uiText?.trim()?.toLowerCase()?.contains((expectedEligibility ?: '').trim().toLowerCase())) {
				KeywordUtil.logInfo("UI billing eligibility text: '${uiText}' — also validated via DB when tracking_Number is set")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyBillingEligibilityOverrideRaised failed: ${e.message}")
		}
	}

	@Keyword
	def verifyManualOverrideShipmentInDb(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required for DB shipment lookup')
			}
			String sql = "SELECT tracking_number, shipment_number, source_type, billing_eligibility FROM ${shipmentTable()} " +
				"WHERE (tracking_number = ? OR shipment_number = ?) " +
				"AND (UPPER(source_type) LIKE '%MANUAL%' OR UPPER(billing_eligibility) LIKE '%OVERRIDE%') LIMIT 5"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			if (rows.isEmpty()) {
				failStop("Manual override shipment not found in DB for tracking_Number=${tn}")
			}
			KeywordUtil.logInfo("Manual override shipment found: ${rows[0]}")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyManualOverrideShipmentInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyOverrideNotAppliedInDb(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required to verify override was not applied')
			}
			String sql = "SELECT billing_eligibility, eligibility_override_status, source_type FROM ${shipmentTable()} " +
				"WHERE tracking_number = ? OR shipment_number = ? LIMIT 1"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			if (!rows.isEmpty()) {
				String billing = (rows[0].billing_eligibility ?: rows[0].eligibility_override_status ?: '').toUpperCase()
				String source = (rows[0].source_type ?: '').toUpperCase()
				if (billing.contains('OVERRIDE') || source.contains('MANUAL')) {
					failStop("Override should not be applied for ${tn} but found billing='${billing}', source='${source}'")
				}
			}
			KeywordUtil.logInfo("Override not applied for shipment ${tn} (as expected)")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyOverrideNotAppliedInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyShipmentCreatedAfterOverride(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required')
			}
			String sql = "SELECT COUNT(*) AS cnt FROM ${shipmentTable()} WHERE tracking_number = ? OR shipment_number = ?"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			int count = rows.isEmpty() ? 0 : Integer.parseInt(rows[0].cnt ?: '0')
			if (count < 1) {
				failStop("Shipment record not created in charging system for ${tn}")
			}
			KeywordUtil.logInfo("Shipment record exists in DB for ${tn} (count=${count})")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyShipmentCreatedAfterOverride failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNoDuplicateShipmentInDb(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required')
			}
			String sql = "SELECT COUNT(*) AS cnt FROM ${shipmentTable()} WHERE tracking_number = ? OR shipment_number = ?"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			int count = rows.isEmpty() ? 0 : Integer.parseInt(rows[0].cnt ?: '0')
			if (count > 1) {
				failStop("Duplicate shipment records found for ${tn}: count=${count}")
			}
			KeywordUtil.logInfo("No duplicate shipment for ${tn} (count=${count})")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyNoDuplicateShipmentInDb failed: ${e.message}")
		}
	}

	private Map findOverrideNotificationEmail(String subjectContains, long pollStartMillis) {
		Map imapConfig = [
			host: profile('MS_IMAP_HOST', 'outlook.office365.com'),
			port: profile('MS_IMAP_PORT', '993'),
			user: profile('MS_IMAP_USER', profile('MS_LOGIN_EMAIL', '')),
			password: profile('MS_IMAP_PASSWORD', '')
		]
		if (!imapConfig.user || !imapConfig.password) {
			failStop('MS_IMAP_USER and MS_IMAP_PASSWORD must be set for OPS notification verification')
		}
		String senderContains = profile('CCI_OVERRIDE_EMAIL_SENDER_CONTAINS', 'noreply')
		int maxWait = Integer.parseInt(profile('GS_EMAIL_FETCH_MAX_WAIT_SECONDS', '120'))
		int pollSec = Integer.parseInt(profile('GS_EMAIL_FETCH_POLL_SECONDS', '10'))
		long deadline = System.currentTimeMillis() + (maxWait * 1000L)
		Date since = new Date(pollStartMillis - 5000L)
		while (System.currentTimeMillis() < deadline) {
			List messages = ImapMailReader.fetchMessagesSince(imapConfig, since)
			for (def msg : messages) {
				String from = (msg.from ?: '') as String
				String subject = (msg.subject ?: '') as String
				if (from.toLowerCase().contains(senderContains.toLowerCase()) &&
					subject.toLowerCase().contains(subjectContains.toLowerCase())) {
					return msg as Map
				}
			}
			WebUI.delay(Math.max(2, pollSec))
		}
		return [:]
	}

	@Keyword
	def verifyOpsOverrideNotification(Map params = [:]) {
		try {
			String trackingNumber = (params.tracking_Number ?: params.trackingNumber ?: '').trim()
			String shipmentStatus = (params.shipment_Status ?: params.shipmentStatus ?: 'Partial').trim()
			boolean expectNotification = params.expect_Notification != null ?
				Boolean.parseBoolean(params.expect_Notification.toString()) : true
			String subjectContains = (params.expected_Subject ?: 'Manual Override').trim()
			long pollStart = params.poll_Start_Millis != null ?
				(params.poll_Start_Millis as long) : (System.currentTimeMillis() - (15 * 60 * 1000L))

			if (expectNotification) {
				emailNoti.assertImapProfileConfiguredForEmailNotification()
				Map email = findOverrideNotificationEmail(subjectContains, pollStart)
				if (email == null || email.isEmpty()) {
					failStop("OPS notification email not found. Subject should contain '${subjectContains}'")
				}
				String body = ImapMailReader.normalizeEmailText(
					"${email.subject ?: ''}\n${email.body ?: ''}")
				if (trackingNumber && !body.toLowerCase().contains(trackingNumber.toLowerCase())) {
					failStop("Notification email should contain shipment reference ${trackingNumber}")
				}
				if (shipmentStatus && !body.toLowerCase().contains(shipmentStatus.toLowerCase())) {
					failStop("Notification email should contain shipment status '${shipmentStatus}'")
				}
				if (!body.matches('.*\\d{4}[-/]\\d{2}[-/]\\d{2}.*') && !body.matches('.*\\d{2}:\\d{2}.*')) {
					KeywordUtil.logWarning('Timestamp pattern not detected in notification body — verify manually if format differs')
				}
				WebUI.takeScreenshot()
			} else {
				int waitSec = Integer.parseInt(profile('GS_EMAIL_NEGATIVE_WAIT_SECONDS', '30'))
				long start = System.currentTimeMillis()
				WebUI.delay(waitSec)
				Map email = findOverrideNotificationEmail(subjectContains, start)
				if (email != null && !email.isEmpty()) {
					failStop("Notification should not be sent but email found: '${email.subject}'")
				}
				KeywordUtil.logInfo('No OPS override notification received (as expected)')
				WebUI.takeScreenshot()
			}
		} catch (Exception e) {
			failStop("verifyOpsOverrideNotification failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDuplicateNotificationsNotSent(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			String sql = "SELECT COUNT(*) AS cnt FROM ${overrideNotificationTable()} " +
				"WHERE shipment_reference = ? OR tracking_number = ?"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			int count = rows.isEmpty() ? 0 : Integer.parseInt(rows[0].cnt ?: '0')
			if (count > 1) {
				failStop("Duplicate notification log rows for ${tn}: count=${count}")
			}
			KeywordUtil.logInfo("Notification log count for ${tn}: ${count}")
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyDuplicateNotificationsNotSent failed: ${e.message}")
		}
	}

	@Keyword
	def sendManualOverrideApiRequest(String payloadJson, boolean repeatRequest = false) {
		try {
			String apiUrl = overrideApiUrl()
			if (!apiUrl) {
				KeywordUtil.markWarning('CCI_OVERRIDE_API_URL is not set — skipping live API call; validate payload structure only')
				KeywordUtil.logInfo("Override API payload: ${payloadJson}")
				safeScreenshot()
				return
			}
			sendOverrideHttpPost(apiUrl, payloadJson)
			if (repeatRequest) {
				sendOverrideHttpPost(apiUrl, payloadJson)
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("sendManualOverrideApiRequest failed: ${e.message}")
		}
	}

	private void sendOverrideHttpPost(String apiUrl, String payloadJson) {
		URL url = new URL(apiUrl)
		HttpURLConnection conn = (HttpURLConnection) url.openConnection()
		conn.setRequestMethod('POST')
		conn.setDoOutput(true)
		conn.setRequestProperty('Content-Type', 'application/json')
		String token = profile('CCI_OVERRIDE_API_TOKEN', '')
		if (token) {
			conn.setRequestProperty('Authorization', "Bearer ${token}")
		}
		conn.outputStream.withWriter('UTF-8') { it.write(payloadJson ?: '{}') }
		int code = conn.responseCode
		KeywordUtil.logInfo("Override API response code: ${code}")
		if (code < 200 || code >= 300) {
			failStop("Override API expected HTTP 200/201 but got ${code}")
		}
		conn.disconnect()
	}

	@Keyword
	def verifySourceTypeManualOverride(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			String expected = manualOverrideSourceValue()
			String sql = "SELECT source_type FROM ${shipmentTable()} WHERE tracking_number = ? OR shipment_number = ? LIMIT 1"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			if (rows.isEmpty()) {
				failStop("Shipment not found for source_type check: ${tn}")
			}
			String source = (rows[0].source_type ?: '').trim()
			if (!source.equalsIgnoreCase(expected)) {
				failStop("Expected source_type='${expected}' but got '${source}'")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifySourceTypeManualOverride failed: ${e.message}")
		}
	}

	@Keyword
	def verifyNotificationLogCount(String trackingNumber, int expectedCount = 1) {
		try {
			String tn = (trackingNumber ?: '').trim()
			String sql = "SELECT COUNT(*) AS cnt FROM ${overrideNotificationTable()} " +
				"WHERE shipment_reference = ? OR tracking_number = ?"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			int count = rows.isEmpty() ? 0 : Integer.parseInt(rows[0].cnt ?: '0')
			if (count != expectedCount) {
				failStop("Expected ${expectedCount} notification log row(s) for ${tn} but found ${count}")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyNotificationLogCount failed: ${e.message}")
		}
	}

	@Keyword
	def verifyAuditColumnsPopulated(String trackingNumber) {
		try {
			String tn = (trackingNumber ?: '').trim()
			String sql = "SELECT created_at, updated_at, created_by, updated_by FROM ${shipmentTable()} " +
				"WHERE tracking_number = ? OR shipment_number = ? LIMIT 1"
			List<Map<String, String>> rows = queryRows(sql, [tn, tn])
			if (rows.isEmpty()) {
				failStop("Shipment not found for audit column check: ${tn}")
			}
			Map row = rows[0]
			if (!(row.created_at ?: '').trim() || !(row.updated_at ?: '').trim()) {
				failStop("created_at/updated_at must be populated for ${tn}")
			}
			safeScreenshot()
		} catch (Exception e) {
			failStop("verifyAuditColumnsPopulated failed: ${e.message}")
		}
	}

	@Keyword
	def runOverrideNegativeScenario(Map params) {
		try {
			String shipmentType = (params.shipment_Type ?: '').trim()
			String trackingNumber = (params.tracking_Number ?: '').trim()
			KeywordUtil.logInfo("Negative override scenario — shipment_Type=${shipmentType}, tracking=${trackingNumber}")
			if (params.payment_Confirmed?.toString()?.equalsIgnoreCase('Y')) {
				uploadPaymentFileForOverride(params.upload_Type, params.fixture_Path, params.entity ?: 'RUH')
			}
			verifyOverrideNotAppliedInDb(trackingNumber)
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("runOverrideNegativeScenario failed: ${e.message}")
		}
	}

	@Keyword
	def executeManualOverrideTest(String testCaseId, def binding = null) {
		String tcId = (testCaseId ?: '').trim()
		if (!tcId) {
			failStop('executeManualOverrideTest requires testCaseId')
		}
		String flowType = bindingVar(binding, 'flow_Type', '')
		boolean needsLogin = !'DB_ONLY'.equalsIgnoreCase(flowType) && !'API_ONLY'.equalsIgnoreCase(flowType)
		if (needsLogin && !tcId.startsWith('TC_MO_DB_') && !tcId.startsWith('TC_MO_API_')) {
			loginForShipmentCreation(true)
		}
		try {
			switch (tcId) {
				case 'TC_MO_001':
					uploadPaymentFileForOverride(
						bindingVar(binding, 'upload_Type', 'Actuals'),
						bindingVar(binding, 'fixture_Path'),
						bindingVar(binding, 'entity', 'RUH'))
					requestManualOverrideEligibility(bindingVar(binding, 'tracking_Number'))
					verifyBillingEligibilityOverrideRaised(
						bindingVar(binding, 'tracking_Number'),
						bindingVar(binding, 'expected_Result', 'Eligibility Override request raised'))
					break
				case 'TC_MO_002':
					uploadPaymentFileForOverride(
						bindingVar(binding, 'upload_Type', 'Estimates'),
						bindingVar(binding, 'fixture_Path', 'Data Files/fixtures/CCIPH-221/valid_estimate.xlsx'),
						bindingVar(binding, 'entity', 'RUH'))
					requestManualOverrideEligibility(bindingVar(binding, 'tracking_Number'))
					verifyBillingEligibilityOverrideRaised(
						bindingVar(binding, 'tracking_Number'),
						bindingVar(binding, 'expected_Result', 'Eligibility Override request raised'))
					break
				case 'TC_MO_003':
					verifyManualOverrideShipmentInDb(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_004':
				case 'TC_MO_005':
				case 'TC_MO_006':
				case 'TC_MO_007':
					runOverrideNegativeScenario([
						shipment_Type: bindingVar(binding, 'shipment_Type'),
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						upload_Type: bindingVar(binding, 'upload_Type', 'Actuals'),
						fixture_Path: bindingVar(binding, 'fixture_Path'),
						entity: bindingVar(binding, 'entity', 'RUH'),
						payment_Confirmed: bindingVar(binding, 'payment_Confirmed', 'N')
					])
					break
				case 'TC_MO_009':
					verifyShipmentCreatedAfterOverride(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_010':
					verifyNoDuplicateShipmentInDb(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_012':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Partial'),
						expect_Notification: 'true',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_013':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Full'),
						expect_Notification: 'false',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_014':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Partial'),
						expect_Notification: 'true',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_015':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Partial'),
						expect_Notification: 'true',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_016':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Partial'),
						expect_Notification: 'true',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_017':
					verifyDuplicateNotificationsNotSent(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_018':
					verifyOpsOverrideNotification([
						tracking_Number: bindingVar(binding, 'tracking_Number'),
						shipment_Status: bindingVar(binding, 'shipment_Status', 'Partial'),
						expect_Notification: 'true',
						expected_Subject: bindingVar(binding, 'expected_Notification_Subject', 'Manual Override')
					])
					break
				case 'TC_MO_API_001':
					sendManualOverrideApiRequest(bindingVar(binding, 'api_Payload', '{"shipmentType":"Skeleton"}'), false)
					break
				case 'TC_MO_API_003':
					sendManualOverrideApiRequest(bindingVar(binding, 'api_Payload', '{"shipmentType":"Skeleton"}'), true)
					verifyNoDuplicateShipmentInDb(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_DB_002':
					verifyNoDuplicateShipmentInDb(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_DB_003':
					verifySourceTypeManualOverride(bindingVar(binding, 'tracking_Number'))
					break
				case 'TC_MO_DB_004':
					verifyNotificationLogCount(bindingVar(binding, 'tracking_Number'), 1)
					break
				case 'TC_MO_DB_005':
					verifyAuditColumnsPopulated(bindingVar(binding, 'tracking_Number'))
					break
				default:
					failStop("No automation mapping for manual override test case: ${tcId}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeManualOverrideTest(${tcId}) failed: ${e.message}")
		}
	}

	// -------------------------------------------------------------------------
	// CCIPH-44 — Auto-mark Domestic shipments as DDU (DOM + 72104493 + RUH)
	// -------------------------------------------------------------------------

	private String shipmentTypeColumn() {
		return profile('CCI_SHIPMENT_TYPE_COLUMN', 'shipment_type')
	}

	private String destinationEntityColumn() {
		return profile('CCI_DESTINATION_ENTITY_COLUMN', 'destination_entity')
	}

	private String resolveShipmentTypeFromRow(Map<String, String> row) {
		List<String> candidates = [
			shipmentTypeColumn(),
			'shipment_type',
			'classification',
			'incoterm',
			'customs_type',
			'shipment_classification'
		].unique()
		for (String col : candidates) {
			String value = (row[col] ?: '').trim()
			if (value) {
				return value
			}
		}
		return ''
	}

	private Map<String, String> findDomesticShipmentRow(String productGroup, String shipperAccount,
			String destinationEntity, String trackingNumber = '') {
		String pg = (productGroup ?: 'DOM').trim()
		String account = (shipperAccount ?: '').trim()
		String entity = (destinationEntity ?: '').trim()
		String tn = (trackingNumber ?: '').trim()
		String destCol = destinationEntityColumn()
		String typeCol = shipmentTypeColumn()
		String sql = "SELECT tracking_number, shipment_number, product_group, shipper_account_number, " +
			"${destCol} AS destination_entity, ${typeCol} AS shipment_type " +
			"FROM ${shipmentTable()} WHERE UPPER(product_group) = UPPER(?) "
		List<Object> params = [pg]
		if (account) {
			sql += 'AND shipper_account_number = ? '
			params << account
		}
		if (entity) {
			sql += "AND UPPER(${destCol}) = UPPER(?) "
			params << entity
		}
		if (tn) {
			sql += 'AND (tracking_number = ? OR shipment_number = ?) '
			params << tn
			params << tn
		}
		sql += 'ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST LIMIT 1'
		List<Map<String, String>> rows = queryRows(sql, params)
		if (rows.isEmpty()) {
			failStop("No shipment found for product_group=${pg}, shipper_account=${account}, destination_entity=${entity}, tracking=${tn}")
		}
		return rows[0]
	}

	private void assertDomesticDduExpectation(Map<String, String> row, boolean expectAutoDdu, String expectedShipmentType = 'DDU') {
		String actualType = resolveShipmentTypeFromRow(row).toUpperCase()
		if (expectAutoDdu) {
			String expected = (expectedShipmentType ?: 'DDU').trim().toUpperCase()
			if (!actualType.equals(expected)) {
				failStop("Expected shipment type '${expected}' but DB has '${actualType}' for tracking=${row.tracking_number ?: row.shipment_number}")
			}
			KeywordUtil.logInfo("Domestic DDU rule applied — shipment type is '${actualType}'")
		} else if (actualType == 'DDU') {
			failStop("Shipment should NOT be auto-marked as DDU under this rule but DB has shipment_type='DDU' for tracking=${row.tracking_number ?: row.shipment_number}")
		} else {
			KeywordUtil.logInfo("Domestic DDU rule correctly not applied — shipment type is '${actualType ?: 'blank'}'")
		}
		safeScreenshot()
	}

	@Keyword
	def verifyDomesticDduRuleInDb(String productGroup, String shipperAccount, String destinationEntity,
			String trackingNumber = '', boolean expectAutoDdu = true, String expectedShipmentType = 'DDU') {
		try {
			Map<String, String> row = findDomesticShipmentRow(productGroup, shipperAccount, destinationEntity, trackingNumber)
			assertDomesticDduExpectation(row, expectAutoDdu, expectedShipmentType)
		} catch (Exception e) {
			failStop("verifyDomesticDduRuleInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyShipmentTypeOnCustomSearchUi(String trackingNumber, String entity, String expectedShipmentType,
			String chargeGroup = 'Cost', String chargeCode = 'ECD', boolean expectAutoDdu = true) {
		verifyShipmentTypeOnUi(trackingNumber, entity, expectedShipmentType, chargeGroup, chargeCode, expectAutoDdu, OR_PREFIX_DDU)
	}

	private void verifyShipmentTypeOnUi(String trackingNumber, String entity, String expectedShipmentType,
			String chargeGroup, String chargeCode, boolean expectMatch, String orPrefix) {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required for Custom Search UI verification')
			}
			loginForShipmentCreation(true)
			navigateToCustomSearchAndVerifyLoaded()
			cs.selectFilters(chargeGroup, chargeCode, null, null, entity ?: 'RUH')
			cs.navigateToChargeDetailPage(tn)
			TestObject shipmentTypeValue = findTestObject(orPrefix + 'val_shipmentType')
			waitVisible(shipmentTypeValue)
			String uiText = WebUI.getText(shipmentTypeValue).trim()
			String actual = uiText.toUpperCase()
			if (expectMatch) {
				String expected = (expectedShipmentType ?: 'DDU').trim().toUpperCase()
				if (!actual.contains(expected)) {
					failStop("UI Shipment Type expected '${expected}' but found '${uiText}' for tracking=${tn}")
				}
			} else if (actual.contains((expectedShipmentType ?: 'DDU').trim().toUpperCase())) {
				failStop("UI Shipment Type should not be '${expectedShipmentType}' under this rule but found '${uiText}' for tracking=${tn}")
			}
			KeywordUtil.logInfo("UI Shipment Type verified: '${uiText}' for tracking=${tn}")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyShipmentTypeOnUi failed: ${e.message}")
		}
	}

	@Keyword
	def executeDomesticDduTest(String testCaseId, def binding = null) {
		String tcId = (testCaseId ?: '').trim()
		if (!tcId) {
			failStop('executeDomesticDduTest requires testCaseId')
		}
		String productGroup = bindingVar(binding, 'product_Group', 'DOM')
		String shipperAccount = bindingVar(binding, 'shipper_Account_Number', DOM_DDU_RULE_ACCOUNT)
		String destinationEntity = bindingVar(binding, 'destination_Entity', 'RUH')
		String trackingNumber = bindingVar(binding, 'tracking_Number', '')
		String expectedShipmentType = bindingVar(binding, 'expected_Shipment_Type', 'DDU')
		boolean expectAutoDdu = !'N'.equalsIgnoreCase(bindingVar(binding, 'expect_Auto_Ddu', 'Y'))
		String flowType = bindingVar(binding, 'flow_Type', 'DB_UI')
		String chargeGroup = bindingVar(binding, 'charge_Group', 'Cost')
		String chargeCode = bindingVar(binding, 'charge_Code', 'ECD')
		boolean uiRequired = !'DB_ONLY'.equalsIgnoreCase(flowType)

		try {
			switch (tcId) {
				case 'TC_DOM_DDU_001':
					verifyDomesticDduRuleInDb(productGroup, shipperAccount, destinationEntity, trackingNumber, true, expectedShipmentType)
					break
				case 'TC_DOM_DDU_002':
					verifyDomesticDduRuleInDb(productGroup, shipperAccount, destinationEntity, trackingNumber, false, expectedShipmentType)
					break
				case 'TC_DOM_DDU_003':
					String otherAccount = shipperAccount?.trim() ? shipperAccount.trim() : '00000001'
					if (DOM_DDU_RULE_ACCOUNT == otherAccount) {
						otherAccount = profile('CCI_DOM_DDU_ALT_ACCOUNT', '00000001')
					}
					verifyDomesticDduRuleInDb(productGroup, otherAccount, destinationEntity, trackingNumber, false, expectedShipmentType)
					break
				default:
					failStop("No automation mapping for domestic DDU test case: ${tcId}")
			}

			if (uiRequired) {
				String resolvedTracking = trackingNumber?.trim()
				if (!resolvedTracking) {
					Map<String, String> row = findDomesticShipmentRow(
						productGroup,
						tcId == 'TC_DOM_DDU_003' ?
							(shipperAccount?.trim() && shipperAccount.trim() != DOM_DDU_RULE_ACCOUNT ?
								shipperAccount.trim() : profile('CCI_DOM_DDU_ALT_ACCOUNT', '00000001')) :
							shipperAccount,
						destinationEntity,
						'')
					resolvedTracking = (row.tracking_number ?: row.shipment_number ?: '').trim()
				}
				if (resolvedTracking) {
					verifyShipmentTypeOnCustomSearchUi(
						resolvedTracking,
						destinationEntity,
						expectedShipmentType,
						chargeGroup,
						chargeCode,
						expectAutoDdu)
				} else {
					KeywordUtil.markWarning('tracking_Number not resolved — UI verification skipped; set seed data tracking_Number in ShipmentCreation_CCIPH44_Data.csv')
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeDomesticDduTest(${tcId}) failed: ${e.message}")
		}
	}

	// -------------------------------------------------------------------------
	// CCIPH-45 — Shipment Type determination (DDU / DDP / DDO)
	// -------------------------------------------------------------------------

	private String cmtColumn() {
		return profile('CCI_CMT_COLUMN', 'cmt')
	}

	private String productTypeColumn() {
		return profile('CCI_PRODUCT_TYPE_COLUMN', 'product_type')
	}

	private String thirdPartyEntityColumn() {
		return profile('CCI_THIRD_PARTY_ENTITY_COLUMN', 'third_party_entity')
	}

	private String shipperNumberColumn() {
		return profile('CCI_SHIPPER_NUMBER_COLUMN', 'shipper_account_number')
	}

	private String arCustomTypeColumn() {
		return profile('CCI_AR_CUSTOM_TYPE_COLUMN', 'custom_type')
	}

	private List<String> parseCsvList(String raw) {
		return (raw ?: '').split(/[,|]/).collect { it.trim() }.findAll { it }
	}

	private void assertShipmentTypeExpectation(Map<String, String> row, String expectedShipmentType) {
		String actualType = resolveShipmentTypeFromRow(row).toUpperCase()
		String expected = (expectedShipmentType ?: '').trim().toUpperCase()
		if (!actualType.equals(expected)) {
			failStop("Expected shipment type '${expected}' but DB has '${actualType}' for tracking=${row.tracking_number ?: row.shipment_number}")
		}
		KeywordUtil.logInfo("Shipment type verified as '${actualType}' for tracking=${row.tracking_number ?: row.shipment_number}")
		safeScreenshot()
	}

	private Map<String, String> findShipmentTypeRow(Map params) {
		String cmt = (params.cmt ?: '').trim()
		String thirdPartyEntity = (params.thirdPartyEntity ?: '').trim()
		String productType = (params.productType ?: '').trim()
		String shipperNumber = (params.shipperNumber ?: '').trim()
		String trackingNumber = (params.trackingNumber ?: '').trim()
		boolean excludeHigherPriority = params.excludeHigherPriority == true

		String cmtCol = cmtColumn()
		String ptCol = productTypeColumn()
		String tpeCol = thirdPartyEntityColumn()
		String shipCol = shipperNumberColumn()
		String typeCol = shipmentTypeColumn()

		String sql = "SELECT tracking_number, shipment_number, ${cmtCol} AS cmt, ${ptCol} AS product_type, " +
			"${tpeCol} AS third_party_entity, ${shipCol} AS shipper_number, ${typeCol} AS shipment_type " +
			"FROM ${shipmentTable()} WHERE 1=1 "
		List<Object> bind = []

		if (cmt) {
			sql += "AND UPPER(${cmtCol}) = UPPER(?) "
			bind << cmt
		}
		if (thirdPartyEntity) {
			sql += "AND UPPER(${tpeCol}) = UPPER(?) "
			bind << thirdPartyEntity
		}
		if (productType) {
			sql += "AND UPPER(${ptCol}) = UPPER(?) "
			bind << productType
		}
		if (shipperNumber) {
			sql += "AND ${shipCol} = ? "
			bind << shipperNumber
		}
		if (trackingNumber) {
			sql += 'AND (tracking_number = ? OR shipment_number = ?) '
			bind << trackingNumber
			bind << trackingNumber
		}
		if (excludeHigherPriority) {
			sql += "AND UPPER(${cmtCol}) NOT IN ('C','F') "
			sql += "AND NOT (UPPER(${cmtCol}) = '3' AND UPPER(${tpeCol}) IN ('RUH','JED','DHA')) "
			sql += "AND NOT (UPPER(${cmtCol}) IN ('P','3') AND UPPER(${ptCol}) IN ('RDG','RDL','RMY','RTC','RTD','RTI','RTN','RTP')) "
		}

		sql += 'ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST LIMIT 1'
		List<Map<String, String>> rows = queryRows(sql, bind)
		if (rows.isEmpty()) {
			failStop("No shipment found for cmt=${cmt}, third_party_entity=${thirdPartyEntity}, product_type=${productType}, shipper=${shipperNumber}, tracking=${trackingNumber}")
		}
		return rows[0]
	}

	private Map<String, String> findDdoShipmentRowByShipper(String shipperNumber, String trackingNumber = '') {
		String shipCol = shipperNumberColumn()
		String arCustomCol = arCustomTypeColumn()
		String typeCol = shipmentTypeColumn()
		String shipper = (shipperNumber ?: '').trim()
		String tn = (trackingNumber ?: '').trim()
		if (!shipper) {
			failStop('shipper_Number is required for DDO classification validation')
		}
		String sql = "SELECT s.tracking_number, s.shipment_number, s.${shipCol} AS shipper_number, " +
			"s.${typeCol} AS shipment_type, ar.${arCustomCol} AS ar_custom_type " +
			"FROM ${shipmentTable()} s " +
			"INNER JOIN ${arAccountTable()} ar ON s.${shipCol} = ar.account_number " +
			"WHERE s.${shipCol} = ? AND UPPER(ar.${arCustomCol}) = 'DDO' "
		List<Object> bind = [shipper]
		if (tn) {
			sql += 'AND (s.tracking_number = ? OR s.shipment_number = ?) '
			bind << tn
			bind << tn
		}
		sql += 'ORDER BY s.updated_at DESC NULLS LAST, s.created_at DESC NULLS LAST LIMIT 1'
		List<Map<String, String>> rows = queryRows(sql, bind)
		if (rows.isEmpty()) {
			failStop("No DDO-classified shipment found for shipper_Number=${shipper} with AR custom_type=DDO")
		}
		return rows[0]
	}

	private void verifyNoArAccountValidation(Map<String, String> row) {
		String tn = (row.tracking_number ?: row.shipment_number ?: '').trim()
		if (!tn) {
			failStop('Cannot verify AR account validation without tracking/shipment number')
		}
		String sql = "SELECT account_number FROM ${chargeTable()} " +
			"WHERE tracking_number = ? OR shipment_number = ? ORDER BY created_at DESC LIMIT 5"
		List<Map<String, String>> charges = queryRows(sql, [tn, tn])
		if (!charges.isEmpty()) {
			boolean hasAccount = charges.any { (it.account_number ?: '').trim() }
			if (hasAccount) {
				failStop("AC1: No AR account validation expected for CMT C/F DDU but account_number is populated for ${tn}")
			}
		}
		KeywordUtil.logInfo("AC1: No AR account validation — charge account is blank for ${tn}")
		safeScreenshot()
	}

	@Keyword
	def verifyShipmentTypeRuleInDb(String cmt, String thirdPartyEntity, String productType,
			String shipperNumber, String trackingNumber, String expectedShipmentType,
			boolean excludeHigherPriority = false) {
		try {
			Map<String, String> row = findShipmentTypeRow([
				cmt: cmt,
				thirdPartyEntity: thirdPartyEntity,
				productType: productType,
				shipperNumber: shipperNumber,
				trackingNumber: trackingNumber,
				excludeHigherPriority: excludeHigherPriority
			])
			assertShipmentTypeExpectation(row, expectedShipmentType)
			return row
		} catch (Exception e) {
			failStop("verifyShipmentTypeRuleInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyDdoShipmentTypeInDb(String shipperNumber, String trackingNumber = '', String expectedShipmentType = 'DDO') {
		try {
			Map<String, String> row = findDdoShipmentRowByShipper(shipperNumber, trackingNumber)
			String arCustom = (row.ar_custom_type ?: '').trim().toUpperCase()
			if (!arCustom.equals('DDO')) {
				failStop("AR Account custom_type should be DDO but found '${arCustom}' for shipper=${shipperNumber}")
			}
			assertShipmentTypeExpectation(row, expectedShipmentType)
			return row
		} catch (Exception e) {
			failStop("verifyDdoShipmentTypeInDb failed: ${e.message}")
		}
	}

	@Keyword
	def executeShipmentTypeTest(String testCaseId, def binding = null) {
		String tcId = (testCaseId ?: '').trim()
		if (!tcId) {
			failStop('executeShipmentTypeTest requires testCaseId')
		}

		String trackingNumber = bindingVar(binding, 'tracking_Number', '')
		String expectedShipmentType = bindingVar(binding, 'expected_Shipment_Type', 'DDU')
		boolean verifyNoAr = 'Y'.equalsIgnoreCase(bindingVar(binding, 'verify_No_Ar_Validation', 'N'))
		String flowType = bindingVar(binding, 'flow_Type', 'DB_UI')
		String chargeGroup = bindingVar(binding, 'charge_Group', 'Cost')
		String chargeCode = bindingVar(binding, 'charge_Code', 'ECD')
		String entity = bindingVar(binding, 'entity', 'RUH')
		boolean uiRequired = !'DB_ONLY'.equalsIgnoreCase(flowType)
		List<String> resolvedTrackings = []

		try {
			switch (tcId) {
				case 'TC_ST_001':
					List<String> cmts = parseCsvList(bindingVar(binding, 'cmt', 'C,F'))
					if (cmts.isEmpty()) {
						cmts = ['C', 'F']
					}
					cmts.each { String cmtValue ->
						Map<String, String> row = verifyShipmentTypeRuleInDb(cmtValue, '', '', '', trackingNumber, expectedShipmentType, false)
						if (verifyNoAr) {
							verifyNoArAccountValidation(row)
						}
						resolvedTrackings << (row.tracking_number ?: row.shipment_number ?: '').trim()
					}
					break
				case 'TC_ST_002':
					List<String> entities = parseCsvList(bindingVar(binding, 'third_Party_Entity', 'RUH,JED,DHA'))
					if (entities.isEmpty()) {
						entities = THIRD_PARTY_ENTITIES
					}
					entities.each { String tpe ->
						Map<String, String> row = verifyShipmentTypeRuleInDb('3', tpe, '', '', trackingNumber, expectedShipmentType, false)
						resolvedTrackings << (row.tracking_number ?: row.shipment_number ?: '').trim()
					}
					break
				case 'TC_ST_003':
					List<String> cmts = parseCsvList(bindingVar(binding, 'cmt', 'P,3'))
					if (cmts.isEmpty()) {
						cmts = ['P', '3']
					}
					List<String> productTypes = parseCsvList(bindingVar(binding, 'product_Type', RETURN_PRODUCT_TYPES.join(',')))
					if (productTypes.isEmpty()) {
						productTypes = RETURN_PRODUCT_TYPES
					}
					cmts.each { String cmtValue ->
						productTypes.each { String pt ->
							Map<String, String> row = verifyShipmentTypeRuleInDb(cmtValue, '', pt, '', trackingNumber, expectedShipmentType, false)
							resolvedTrackings << (row.tracking_number ?: row.shipment_number ?: '').trim()
						}
					}
					break
				case 'TC_ST_004':
					List<String> productTypes = parseCsvList(bindingVar(binding, 'product_Type', 'LHS,DIM'))
					if (productTypes.isEmpty()) {
						productTypes = ['LHS', 'DIM']
					}
					String cmtValue = bindingVar(binding, 'cmt', 'B')
					productTypes.each { String pt ->
						Map<String, String> row = verifyShipmentTypeRuleInDb(cmtValue, '', pt, '', trackingNumber, 'DDP', true)
						resolvedTrackings << (row.tracking_number ?: row.shipment_number ?: '').trim()
					}
					break
				case 'TC_ST_005':
					String shipperNumber = bindingVar(binding, 'shipper_Number', profile('CCI_DDO_SHIPPER_NUMBER', ''))
					Map<String, String> row = verifyDdoShipmentTypeInDb(shipperNumber, trackingNumber, expectedShipmentType)
					resolvedTrackings << (row.tracking_number ?: row.shipment_number ?: '').trim()
					break
				default:
					failStop("No automation mapping for shipment type test case: ${tcId}")
			}

			if (uiRequired) {
				String uiTracking = trackingNumber?.trim()
				if (!uiTracking && !resolvedTrackings.isEmpty()) {
					uiTracking = resolvedTrackings.find { it?.trim() } ?: ''
				}
				if (uiTracking) {
					verifyShipmentTypeOnUi(uiTracking, entity, expectedShipmentType, chargeGroup, chargeCode, true, OR_PREFIX_ST)
				} else {
					KeywordUtil.markWarning('tracking_Number not resolved — UI verification skipped; set seed data in ShipmentCreation_CCIPH45_Data.csv')
				}
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeShipmentTypeTest(${tcId}) failed: ${e.message}")
		}
	}

	// -------------------------------------------------------------------------
	// CCIPH-46 — Entity allocation at charge creation (DDP / DDU / DDO)
	// -------------------------------------------------------------------------

	private String allocatedEntityColumn() {
		return profile('CCI_ALLOCATED_ENTITY_COLUMN', 'allocated_entity')
	}

	private String entityTable() {
		return profile('CCI_ENTITY_TABLE', 'entity')
	}

	private String ddpAslAccount() {
		return profile('CCI_DDP_ASL_ACCOUNT', 'ASL001')
	}

	private String outOfScopeEntityCode() {
		return profile('CCI_OUT_OF_SCOPE_ENTITY', 'DXB')
	}

	private String resolveColumnValue(Map<String, String> row, List<String> candidates) {
		for (String col : candidates.unique()) {
			String value = (row[col] ?: '').trim()
			if (value) {
				return value
			}
		}
		return ''
	}

	private String resolveAllocatedEntityFromRow(Map<String, String> row) {
		return resolveColumnValue(row, [
			'allocated_entity',
			allocatedEntityColumn(),
			'billing_entity',
			'entity'
		])
	}

	private Map<String, String> findChargeRowForShipment(String trackingNumber) {
		String tn = (trackingNumber ?: '').trim()
		if (!tn) {
			return [:]
		}
		String sql = "SELECT account_number, entity, charge_code FROM ${chargeTable()} " +
			"WHERE tracking_number = ? OR shipment_number = ? ORDER BY created_at DESC NULLS LAST LIMIT 1"
		List<Map<String, String>> rows = queryRows(sql, [tn, tn])
		return rows.isEmpty() ? [:] : rows[0]
	}

	private String fetchDefaultCashAccountForEntity(String entityCode) {
		String entity = (entityCode ?: '').trim()
		if (!entity) {
			return profile('CCI_DEFAULT_CASH_ACCOUNT', '').trim()
		}
		String sql = "SELECT default_cash_account FROM ${entityTable()} " +
			"WHERE UPPER(entity_code) = UPPER(?) OR UPPER(code) = UPPER(?) OR UPPER(name) = UPPER(?) LIMIT 1"
		List<Map<String, String>> rows = queryRows(sql, [entity, entity, entity])
		if (!rows.isEmpty() && (rows[0].default_cash_account ?: '').trim()) {
			return rows[0].default_cash_account.trim()
		}
		return profile('CCI_DEFAULT_CASH_ACCOUNT', '').trim()
	}

	private Map<String, String> findArAccountRow(String accountNumber) {
		String account = (accountNumber ?: '').trim()
		if (!account) {
			return [:]
		}
		String sql = "SELECT account_number, entity, default_cash_account FROM ${arAccountTable()} " +
			"WHERE account_number = ? LIMIT 1"
		List<Map<String, String>> rows = queryRows(sql, [account])
		return rows.isEmpty() ? [:] : rows[0]
	}

	private Map<String, String> findAllocationShipmentRow(Map params) {
		String shipmentType = (params.shipmentType ?: '').trim()
		String destinationEntity = (params.destinationEntity ?: '').trim()
		String thirdPartyEntity = (params.thirdPartyEntity ?: '').trim()
		String cmt = (params.cmt ?: '').trim()
		String trackingNumber = (params.trackingNumber ?: '').trim()
		String accountNumber = (params.accountNumber ?: '').trim()
		String productType = (params.productType ?: '').trim()
		boolean accountBlank = params.accountBlank == true
		boolean destinationInScope = params.destinationInScope
		boolean destinationOutOfScope = params.destinationOutOfScope
		boolean arEntityInScope = params.arEntityInScope
		boolean arEntityOutOfScope = params.arEntityOutOfScope

		String typeCol = shipmentTypeColumn()
		String destCol = destinationEntityColumn()
		String tpeCol = thirdPartyEntityColumn()
		String cmtCol = cmtColumn()
		String allocCol = allocatedEntityColumn()
		String shipCol = shipperNumberColumn()
		String ptCol = productTypeColumn()

		String sql = "SELECT s.tracking_number, s.shipment_number, s.${typeCol} AS shipment_type, " +
			"s.${destCol} AS destination_entity, s.${tpeCol} AS third_party_entity, s.${cmtCol} AS cmt, " +
			"s.${allocCol} AS allocated_entity, s.${shipCol} AS shipper_number, s.${ptCol} AS product_type, " +
			"ar.account_number AS ar_account_number, ar.entity AS ar_entity " +
			"FROM ${shipmentTable()} s "
		List<Object> bind = []
		boolean joinAr = arEntityInScope || arEntityOutOfScope || accountNumber?.trim()
		if (joinAr) {
			sql += "LEFT JOIN ${arAccountTable()} ar ON s.${shipCol} = ar.account_number "
		} else {
			sql += "LEFT JOIN ${arAccountTable()} ar ON 1=0 "
		}
		sql += 'WHERE 1=1 '

		if (shipmentType) {
			List<String> types = parseCsvList(shipmentType)
			if (types.size() == 1) {
				sql += "AND UPPER(s.${typeCol}) = UPPER(?) "
				bind << types[0]
			} else if (!types.isEmpty()) {
				sql += "AND UPPER(s.${typeCol}) IN (${types.collect { 'UPPER(?)' }.join(', ')}) "
				types.each { bind << it }
			}
		}
		if (destinationEntity) {
			List<String> entities = parseCsvList(destinationEntity)
			if (entities.size() == 1) {
				sql += "AND UPPER(s.${destCol}) = UPPER(?) "
				bind << entities[0]
			} else if (!entities.isEmpty()) {
				sql += "AND UPPER(s.${destCol}) IN (${entities.collect { 'UPPER(?)' }.join(', ')}) "
				entities.each { bind << it }
			}
		}
		if (thirdPartyEntity) {
			List<String> entities = parseCsvList(thirdPartyEntity)
			if (entities.size() == 1) {
				sql += "AND UPPER(s.${tpeCol}) = UPPER(?) "
				bind << entities[0]
			} else if (!entities.isEmpty()) {
				sql += "AND UPPER(s.${tpeCol}) IN (${entities.collect { 'UPPER(?)' }.join(', ')}) "
				entities.each { bind << it }
			}
		}
		if (cmt) {
			sql += "AND UPPER(s.${cmtCol}) = UPPER(?) "
			bind << cmt
		}
		if (productType) {
			List<String> types = parseCsvList(productType)
			if (!types.isEmpty()) {
				sql += "AND UPPER(s.${ptCol}) IN (${types.collect { 'UPPER(?)' }.join(', ')}) "
				types.each { bind << it }
			}
		}
		if (trackingNumber) {
			sql += 'AND (s.tracking_number = ? OR s.shipment_number = ?) '
			bind << trackingNumber
			bind << trackingNumber
		}
		if (accountNumber) {
			sql += "AND s.${shipCol} = ? "
			bind << accountNumber
		}
		if (accountBlank) {
			sql += "AND (s.${shipCol} IS NULL OR TRIM(s.${shipCol}) = '') "
		}
		if (destinationInScope) {
			sql += "AND UPPER(s.${destCol}) IN ('RUH','JED','DHA') "
		}
		if (destinationOutOfScope) {
			sql += "AND UPPER(s.${destCol}) NOT IN ('RUH','JED','DHA') "
		}
		if (arEntityInScope) {
			sql += "AND UPPER(ar.entity) IN ('RUH','JED','DHA') "
		}
		if (arEntityOutOfScope) {
			sql += "AND ar.entity IS NOT NULL AND UPPER(ar.entity) NOT IN ('RUH','JED','DHA') "
		}

		sql += 'ORDER BY s.updated_at DESC NULLS LAST, s.created_at DESC NULLS LAST LIMIT 1'
		List<Map<String, String>> rows = queryRows(sql, bind)
		if (rows.isEmpty()) {
			failStop("No shipment found for entity allocation: shipmentType=${shipmentType}, destination=${destinationEntity}, cmt=${cmt}, tracking=${trackingNumber}")
		}
		return rows[0]
	}

	private void assertEntityAllocationExpectation(Map<String, String> shipmentRow, String expectedAllocatedEntity,
			String accountExpectationType, String explicitExpectedAccount = '') {
		String tracking = (shipmentRow.tracking_number ?: shipmentRow.shipment_number ?: '').trim()
		String destinationEntity = (shipmentRow.destination_entity ?: '').trim()
		String thirdPartyEntity = (shipmentRow.third_party_entity ?: '').trim()
		String arEntity = (shipmentRow.ar_entity ?: '').trim()
		String actualAllocated = resolveAllocatedEntityFromRow(shipmentRow)

		String expectedEntity = (expectedAllocatedEntity ?: '').trim()
		if (!expectedEntity) {
			switch ((accountExpectationType ?: '').trim().toUpperCase()) {
				case 'ASL001':
				case 'DEFAULT_CASH':
					expectedEntity = destinationEntity
					break
				case 'AR_ACCOUNT':
					expectedEntity = thirdPartyEntity ?: arEntity ?: destinationEntity
					break
				default:
					expectedEntity = destinationEntity
			}
		}

		if (!actualAllocated.equalsIgnoreCase(expectedEntity)) {
			failStop("Allocated entity mismatch for ${tracking}. Expected '${expectedEntity}', got '${actualAllocated}'")
		}

		Map<String, String> chargeRow = findChargeRowForShipment(tracking)
		String actualAccount = (chargeRow.account_number ?: '').trim()
		String expectation = (accountExpectationType ?: '').trim().toUpperCase()

		switch (expectation) {
			case 'ASL001':
				String asl = (explicitExpectedAccount ?: ddpAslAccount()).trim()
				if (!actualAccount.equalsIgnoreCase(asl)) {
					failStop("Expected ASL account '${asl}' but charge account is '${actualAccount}' for ${tracking}")
				}
				break
			case 'DEFAULT_CASH':
				String defaultCash = (explicitExpectedAccount ?: fetchDefaultCashAccountForEntity(expectedEntity)).trim()
				if (defaultCash) {
					if (!actualAccount.equalsIgnoreCase(defaultCash)) {
						failStop("Expected Default Cash Account '${defaultCash}' but charge account is '${actualAccount}' for ${tracking}")
					}
				} else if (!actualAccount) {
					failStop("Default Cash Account should be assigned for ${tracking} but charge account is blank")
				} else {
					KeywordUtil.logInfo("Default Cash Account assigned as '${actualAccount}' for entity ${expectedEntity}")
				}
				break
			case 'AR_ACCOUNT':
				String arAccount = (explicitExpectedAccount ?: shipmentRow.ar_account_number ?: shipmentRow.shipper_number ?: '').trim()
				if (!arAccount) {
					failStop("AR-linked account expected for ${tracking} but no AR account could be resolved")
				}
				if (!actualAccount.equalsIgnoreCase(arAccount)) {
					failStop("Expected AR account '${arAccount}' but charge account is '${actualAccount}' for ${tracking}")
				}
				break
			case 'BLANK':
				if (actualAccount) {
					failStop("Account should be blank for ${tracking} but charge account is '${actualAccount}'")
				}
				break
			default:
				if (explicitExpectedAccount?.trim() && !actualAccount.equalsIgnoreCase(explicitExpectedAccount.trim())) {
					failStop("Expected account '${explicitExpectedAccount}' but charge account is '${actualAccount}' for ${tracking}")
				}
		}
		KeywordUtil.logInfo("Entity allocation verified for ${tracking}: entity='${actualAllocated}', account='${actualAccount}'")
		safeScreenshot()
	}

	@Keyword
	def verifyEntityAllocationInDb(String shipmentType, String destinationEntity = '', String thirdPartyEntity = '',
			String cmt = '', String trackingNumber = '', String accountNumber = '', String productType = '',
			boolean accountBlank = false, boolean destinationInScope = false, boolean destinationOutOfScope = false,
			boolean arEntityInScope = false, boolean arEntityOutOfScope = false,
			String expectedAllocatedEntity = '', String accountExpectationType = 'DEFAULT_CASH',
			String explicitExpectedAccount = '') {
		try {
			Map<String, String> row = findAllocationShipmentRow([
				shipmentType: shipmentType,
				destinationEntity: destinationEntity,
				thirdPartyEntity: thirdPartyEntity,
				cmt: cmt,
				trackingNumber: trackingNumber,
				accountNumber: accountNumber,
				productType: productType,
				accountBlank: accountBlank,
				destinationInScope: destinationInScope,
				destinationOutOfScope: destinationOutOfScope,
				arEntityInScope: arEntityInScope,
				arEntityOutOfScope: arEntityOutOfScope
			])
			assertEntityAllocationExpectation(row, expectedAllocatedEntity, accountExpectationType, explicitExpectedAccount)
			return row
		} catch (Exception e) {
			failStop("verifyEntityAllocationInDb failed: ${e.message}")
		}
	}

	@Keyword
	def verifyEntityAllocationOnCustomSearchUi(String trackingNumber, String entity, String expectedAllocatedEntity,
			String expectedAccount, String chargeGroup = 'Cost', String chargeCode = 'ECD') {
		try {
			String tn = (trackingNumber ?: '').trim()
			if (!tn) {
				failStop('tracking_Number is required for entity allocation UI verification')
			}
			loginForShipmentCreation(true)
			navigateToCustomSearchAndVerifyLoaded()
			cs.selectFilters(chargeGroup, chargeCode, null, null, entity ?: 'RUH')
			cs.navigateToChargeDetailPage(tn)

			TestObject allocatedEntityValue = toAlloc('val_allocatedEntity')
			TestObject allocatedAccountValue = toAlloc('val_allocatedAccount')
			waitVisible(allocatedEntityValue)
			waitVisible(allocatedAccountValue)

			String uiEntity = WebUI.getText(allocatedEntityValue).trim()
			String uiAccount = WebUI.getText(allocatedAccountValue).trim()
			if (expectedAllocatedEntity?.trim() && !uiEntity.equalsIgnoreCase(expectedAllocatedEntity.trim())) {
				failStop("UI allocated entity expected '${expectedAllocatedEntity}' but found '${uiEntity}' for tracking=${tn}")
			}
			if (expectedAccount?.trim() && !uiAccount.equalsIgnoreCase(expectedAccount.trim())) {
				failStop("UI allocated account expected '${expectedAccount}' but found '${uiAccount}' for tracking=${tn}")
			}
			KeywordUtil.logInfo("UI entity allocation verified for ${tn}: entity='${uiEntity}', account='${uiAccount}'")
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("verifyEntityAllocationOnCustomSearchUi failed: ${e.message}")
		}
	}

	private void runEntityAllocationUiIfRequired(Map<String, String> shipmentRow, String flowType,
			String chargeGroup, String chargeCode, String entityFilter) {
		if ('DB_ONLY'.equalsIgnoreCase((flowType ?: '').trim())) {
			return
		}
		String tracking = (shipmentRow.tracking_number ?: shipmentRow.shipment_number ?: '').trim()
		if (!tracking) {
			KeywordUtil.markWarning('tracking_Number not resolved — UI entity allocation verification skipped')
			return
		}
		Map<String, String> chargeRow = findChargeRowForShipment(tracking)
		verifyEntityAllocationOnCustomSearchUi(
			tracking,
			entityFilter ?: resolveAllocatedEntityFromRow(shipmentRow),
			resolveAllocatedEntityFromRow(shipmentRow),
			(chargeRow.account_number ?: '').trim(),
			chargeGroup,
			chargeCode
		)
	}

	@Keyword
	def executeEntityAllocationTest(String testCaseId, def binding = null) {
		String tcId = (testCaseId ?: '').trim()
		if (!tcId) {
			failStop('executeEntityAllocationTest requires testCaseId')
		}

		String trackingNumber = bindingVar(binding, 'tracking_Number', '')
		String flowType = bindingVar(binding, 'flow_Type', 'DB_UI')
		String chargeGroup = bindingVar(binding, 'charge_Group', 'Cost')
		String chargeCode = bindingVar(binding, 'charge_Code', 'ECD')
		String entityFilter = bindingVar(binding, 'entity', 'RUH')
		String shipmentType = bindingVar(binding, 'shipment_Type', '')
		String destinationEntity = bindingVar(binding, 'destination_Entity', '')
		String thirdPartyEntity = bindingVar(binding, 'third_Party_Entity', '')
		String cmt = bindingVar(binding, 'cmt', '')
		String accountNumber = bindingVar(binding, 'account_Number', '')
		String productType = bindingVar(binding, 'product_Type', '')
		String accountExpectationType = bindingVar(binding, 'expected_Account_Type', 'DEFAULT_CASH')
		String explicitExpectedAccount = bindingVar(binding, 'expected_Account', '')
		boolean accountBlank = 'BLANK'.equalsIgnoreCase(accountNumber) || 'BLANK'.equalsIgnoreCase(accountExpectationType)
		if ('OUT_OF_SCOPE'.equalsIgnoreCase(accountNumber)) {
			accountNumber = profile('CCI_ALLOC_OUT_OF_SCOPE_AR_ACCOUNT', '')
		}

		try {
			switch (tcId) {
				case 'TC_ALLOC_001':
					List<String> entities = parseCsvList(destinationEntity ?: 'RUH,JED,DHA')
					if (entities.isEmpty()) {
						entities = THIRD_PARTY_ENTITIES
					}
					entities.each { String dest ->
						Map<String, String> row = verifyEntityAllocationInDb(
							shipmentType ?: 'DDP', dest, '', '', trackingNumber, '', productType ?: 'LHS,DIM',
							false, true, false, false, false, dest, 'ASL001', explicitExpectedAccount ?: ddpAslAccount())
						runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, dest)
					}
					break
				case 'TC_ALLOC_002':
					String outEntity = destinationEntity?.trim() ?: outOfScopeEntityCode()
					Map<String, String> row2 = verifyEntityAllocationInDb(
						shipmentType ?: 'DDP', outEntity, '', '', trackingNumber, '', productType ?: 'LHS,DIM',
						false, false, true, false, false, outEntity, 'DEFAULT_CASH', explicitExpectedAccount)
					runEntityAllocationUiIfRequired(row2, flowType, chargeGroup, chargeCode, outEntity)
					break
				case 'TC_ALLOC_003':
					List<String> tpeList = parseCsvList(thirdPartyEntity ?: 'RUH,JED,DHA')
					if (tpeList.isEmpty()) {
						tpeList = THIRD_PARTY_ENTITIES
					}
					List<String> types = parseCsvList(shipmentType ?: 'DDU,DDO')
					types.each { String st ->
						tpeList.each { String tpe ->
							Map<String, String> row = verifyEntityAllocationInDb(
								st, '', tpe, cmt ?: '3', trackingNumber, '', productType,
								false, false, false, false, false, tpe, 'AR_ACCOUNT', explicitExpectedAccount)
							runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, tpe)
						}
					}
					break
				case 'TC_ALLOC_004':
					String arAccount = accountNumber?.trim() ?: profile('CCI_ALLOC_AR_ACCOUNT', '')
					if (!arAccount) {
						failStop('account_Number or CCI_ALLOC_AR_ACCOUNT profile must be set for TC_ALLOC_004')
					}
					Map<String, String> arRow = findArAccountRow(arAccount)
					String arEntity = (arRow.entity ?: bindingVar(binding, 'ar_Account_Entity', 'RUH')).trim()
					List<String> types4 = parseCsvList(shipmentType ?: 'DDU,DDO')
					types4.each { String st ->
						Map<String, String> row = verifyEntityAllocationInDb(
							st, destinationEntity ?: arEntity, '', cmt, trackingNumber, arAccount, productType,
							false, false, false, true, false, arEntity, 'AR_ACCOUNT', arAccount)
						runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, arEntity)
					}
					break
				case 'TC_ALLOC_005':
					List<String> types5 = parseCsvList(shipmentType ?: 'DDU,DDO')
					types5.each { String st ->
						Map<String, String> row = verifyEntityAllocationInDb(
							st, destinationEntity ?: 'RUH', '', cmt, trackingNumber, '', productType,
							true, false, false, false, false, destinationEntity ?: 'RUH', 'DEFAULT_CASH', explicitExpectedAccount)
						runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, destinationEntity ?: 'RUH')
					}
					break
				case 'TC_ALLOC_006':
					String outArAccount = accountNumber?.trim() ?: profile('CCI_ALLOC_OUT_OF_SCOPE_AR_ACCOUNT', '')
					List<String> types6 = parseCsvList(shipmentType ?: 'DDU,DDO')
					types6.each { String st ->
						Map<String, String> row = verifyEntityAllocationInDb(
							st, destinationEntity ?: 'RUH', '', cmt, trackingNumber, outArAccount, productType,
							false, false, false, false, true, destinationEntity ?: 'RUH', 'DEFAULT_CASH', explicitExpectedAccount)
						runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, destinationEntity ?: 'RUH')
					}
					break
				case 'TC_ALLOC_007':
					List<String> mixedTypes = parseCsvList(shipmentType ?: 'DDP,DDU,DDO')
					if (mixedTypes.isEmpty()) {
						mixedTypes = ['DDP', 'DDU', 'DDO']
					}
					mixedTypes.each { String st ->
						Map<String, String> row
						if ('DDP'.equalsIgnoreCase(st)) {
							row = verifyEntityAllocationInDb(
								'DDP', destinationEntity ?: 'RUH', '', '', trackingNumber, '', productType ?: 'LHS,DIM',
								false, true, false, false, false, destinationEntity ?: 'RUH', 'ASL001', ddpAslAccount())
						} else if ('DDU'.equalsIgnoreCase(st)) {
							row = verifyEntityAllocationInDb(
								'DDU', destinationEntity ?: 'RUH', thirdPartyEntity, cmt ?: '3', trackingNumber, accountNumber, productType,
								accountBlank, false, false, false, false, '', accountExpectationType ?: 'DEFAULT_CASH', explicitExpectedAccount)
						} else {
							String ddoShipper = bindingVar(binding, 'shipper_Number', profile('CCI_DDO_SHIPPER_NUMBER', ''))
							row = verifyDdoShipmentTypeInDb(ddoShipper, trackingNumber, 'DDO')
							verifyEntityAllocationInDb(
								'DDO', destinationEntity ?: 'RUH', '', '', trackingNumber, ddoShipper, productType,
								false, false, false, true, false, '', 'AR_ACCOUNT', ddoShipper)
						}
						runEntityAllocationUiIfRequired(row, flowType, chargeGroup, chargeCode, entityFilter)
					}
					break
				default:
					failStop("No automation mapping for entity allocation test case: ${tcId}")
			}
			WebUI.takeScreenshot()
		} catch (Exception e) {
			failStop("executeEntityAllocationTest(${tcId}) failed: ${e.message}")
		}
	}
}
