package apmanagement

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.testdata.TestData

/**
 * Test data for AP Management CCIPH-102 (CSV-backed).
 * Source: {@code Data Files/AP_Management_CCIPH102_Data}
 */
class ApCciph102Data {

	private static final String DATA_FILE_PATH = 'Data Files/AP_Management_CCIPH102_Data'

	static final String COL_TEST_CASE_ID = 'testCaseId'
	static final String COL_SEARCH_CRITERIA = 'searchCriteria'
	static final String COL_SEARCH_BY = 'searchBy'
	static final String COL_ENTITY = 'entity'
	static final String COL_SEARCH_INPUT = 'searchInput'

	private static TestData dataFile() {
		return findTestData(DATA_FILE_PATH)
	}

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
			String cellId = cell(data, row, COL_TEST_CASE_ID, 'ID', 'id')
			if (cellId.equalsIgnoreCase(id)) {
				return row
			}
		}
		throw new IllegalStateException("No row found in ${DATA_FILE_PATH} for testCaseId: ${id}")
	}

	static Map<String, String> forTestCase(String testCaseId) {
		int row = rowIndex(testCaseId)
		TestData data = dataFile()
		return [
			(COL_TEST_CASE_ID)   : cell(data, row, COL_TEST_CASE_ID, 'ID'),
			(COL_SEARCH_CRITERIA): cell(data, row, COL_SEARCH_CRITERIA, 'SearchCriteria'),
			(COL_SEARCH_BY)      : cell(data, row, COL_SEARCH_BY, 'SearchBy', 'searchType'),
			(COL_ENTITY)         : cell(data, row, COL_ENTITY, 'Entity'),
			(COL_SEARCH_INPUT)   : cell(data, row, COL_SEARCH_INPUT, 'searchText', 'input'),
		]
	}
}
