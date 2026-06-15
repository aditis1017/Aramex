package globalsettings

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.testdata.TestData

/**
 * Data access for Test Cases/GS/User-CCIPH-30.
 * Primary source: {@code Data Files/GlobalSettings_User_CCIPH30_Data} (CSV), aligned with
 * column names in {@code Include/GlobalSettings-Data.xlsx} ({@code tab}, {@code targetValue}, …) plus {@code testCaseId}.
 * Legacy column names ({@code GS_TAB}, …) are still read if present.
 */
class GsUserCciph30Data {

	/** CSV / Excel-backed test data (re-export from Include/GlobalSettings-Data.xlsx when that file changes). */
	private static final String DATA_FILE_PATH = 'Data Files/GlobalSettings_User_CCIPH30_Data'

	static final String COL_TEST_CASE_ID = 'testCaseId'
	static final String COL_TAB = 'GS_TAB'
	static final String COL_DESTINATION = 'GS_DESTINATION'
	static final String COL_CONDITION_FIELD = 'GS_CONDITION_FIELD'
	static final String COL_CONDITION_OPERATOR = 'GS_CONDITION_OPERATOR'
	static final String COL_TARGET_VALUE = 'GS_TARGET_VALUE'

	private static TestData dataFile() {
		return findTestData(DATA_FILE_PATH)
	}

	/** First non-empty value among candidate column names (supports Excel + legacy headers). */
	private static String cell(TestData data, int row, String... columnNames) {
		for (String col : columnNames) {
			try {
				String v = data.getValue(col, row)
				if (v != null && !v.toString().trim().isEmpty()) {
					return v.toString().trim()
				}
			} catch (Exception ignored) {
			}
		}
		return ''
	}

	private static int rowIndex(String testCaseId) {
		String id = (testCaseId ?: '').trim()
		if (!id) {
			throw new IllegalArgumentException('testCaseId is required')
		}
		TestData data = dataFile()
		for (int row : data.getRowNumbers()) {
			String cellId = cell(data, row, COL_TEST_CASE_ID, 'TestCaseId', 'TC_ID', 'ID')
			if (cellId.equalsIgnoreCase(id)) {
				return row
			}
		}
		throw new IllegalStateException("No row found in ${DATA_FILE_PATH} for testCaseId: ${id}")
	}

	static String get(String testCaseId, String columnName) {
		return cell(dataFile(), rowIndex(testCaseId), columnName)
	}

	/** Returns static columns for a test case (short keys match Excel-style names). */
	static Map<String, String> forTestCase(String testCaseId) {
		int row = rowIndex(testCaseId)
		TestData data = dataFile()
		String tab = cell(data, row, 'tab', COL_TAB)
		String destination = cell(data, row, 'destination', COL_DESTINATION)
		String conditionField = cell(data, row, 'conditionField', COL_CONDITION_FIELD)
		String conditionOperator = cell(data, row, 'conditionOperator', COL_CONDITION_OPERATOR)
		String targetValue = cell(data, row, 'targetValue', COL_TARGET_VALUE)
		String valuesComparator = cell(data, row, 'valuesComparator', 'GS_VALUES_OPERATOR', 'valueOperator')
		String id = cell(data, row, COL_TEST_CASE_ID, 'TestCaseId', 'TC_ID')
		return [
			(COL_TEST_CASE_ID)       : id,
			(COL_TAB)                : tab,
			(COL_DESTINATION)        : destination,
			(COL_CONDITION_FIELD)    : conditionField,
			(COL_CONDITION_OPERATOR) : conditionOperator,
			(COL_TARGET_VALUE)       : targetValue,
			'valuesComparator'       : valuesComparator,
			'tab'                    : tab,
			'destination'            : destination,
			'conditionField'         : conditionField,
			'conditionOperator'      : conditionOperator,
			'targetValue'            : targetValue,
		]
	}

	static String randomRuleValue() {
		return String.valueOf(System.currentTimeMillis() % 100000)
	}

	static String randomUpdatedRuleValue() {
		return String.valueOf(System.currentTimeMillis() % 100000 + 1)
	}
}
