package com.customcharge.dashboard.constants;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Direct import screen, column, and validation constants.
 */
public final class ImportConstants {

    public static final String SCREEN_RATE_SHEET = "RATE_SHEET";
    public static final String SCREEN_B2B_CONDITION = "B2B_CONDITION";

    public static final String MANDATORY_COLUMNS_SUFFIX = "_MANDATORY_COLUMNS_";
    public static final String CHARGE_SUFFIX = "_Charge";

    public static final String CLASSPATH_SCREENS_LOCATION = "direct-import-screens.yml";
    public static final String YAML_SCREENS_KEY = "screens";

    public static final String CLASSPATH_TEMPLATE_PREFIX = "classpath:template/";
    public static final String S3_TEMPLATE_PREFIX = "template";
    public static final String S3_GLOBAL_FOLDER = "globleFiles";

    public static final String PARAM_RATE_SHEET_ID = "rateSheetId";
    public static final String PARAM_SCREEN_NAME = "screenName";
    public static final String PARAM_PARAMS = "params";

    public static final String FILE_TYPE_ACTUAL = "actual";
    public static final String FILE_TYPE_ESTIMATE = "estimate";

    public static final String EXTENSION_CSV = ".csv";
    public static final String EXTENSION_XLSX = ".xlsx";
    public static final String EXTENSION_XLS = ".xls";

    public static final String RATE_TYPE_COLUMN_PREFIX = "Rate Type ";
    public static final String PERCENTAGE_OF_COLUMN_PREFIX = "Percentage Of ";
    public static final String VALUE_COLUMN_PREFIX = "Value ";

    public static final int MAX_RATE_SLOTS = 3;
    public static final int KEYWORD_MIN_LENGTH = 2;
    public static final int DATA_ROW_NUMBER_OFFSET = 2;

    public static final Pattern KEYWORD_ALLOWED_CHARACTERS =
            Pattern.compile("^[\\p{IsAlphabetic}\\p{IsDigit}&@.\\s]+$");
    public static final Pattern MATCH_TYPE_NORMALIZE_PATTERN = Pattern.compile("[\\s_-]+");
    public static final Pattern PATH_SEGMENT_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    public static final String MATCH_TYPE_CONTAINS = "CONTAINS";
    public static final String MATCH_TYPE_EXACT = "EXACT";
    public static final String MATCH_TYPE_STARTWITH = "START WITH";
    public static final String MATCH_TYPE_ENDWITH = "END WITH";

    public static final List<String> ALLOWED_MATCH_TYPE_LABELS = List.of(
            MATCH_TYPE_CONTAINS,
            MATCH_TYPE_EXACT,
            MATCH_TYPE_STARTWITH,
            MATCH_TYPE_ENDWITH);

    public static final Set<String> ALLOWED_MATCH_TYPES_NORMALIZED = Set.of(
            normalizeMatchType(MATCH_TYPE_CONTAINS),
            normalizeMatchType(MATCH_TYPE_EXACT),
            normalizeMatchType(MATCH_TYPE_STARTWITH),
            normalizeMatchType(MATCH_TYPE_ENDWITH));

    public static final String ALLOWED_MATCH_TYPE_LABELS_TEXT =
            ALLOWED_MATCH_TYPE_LABELS.stream().collect(Collectors.joining(", "));

    public static final String ACTIVE_TRUE = "true";
    public static final String ACTIVE_YES = "yes";
    public static final String ACTIVE_Y = "y";
    public static final String ACTIVE_ONE = "1";
    public static final String ACTIVE_ON = "on";
    public static final String ACTIVE_ACTIVE = "active";

    public static final String ACTIVE_FALSE = "false";
    public static final String ACTIVE_NO = "no";
    public static final String ACTIVE_N = "n";
    public static final String ACTIVE_ZERO = "0";
    public static final String ACTIVE_OFF = "off";
    public static final String ACTIVE_INACTIVE = "inactive";

    public static final String DEDUPE_KEY_SEPARATOR = "::";

    public static final String MESSAGE_FILE_REQUIRED = "A file must be uploaded.";
    public static final String MESSAGE_FILE_EMPTY = "The uploaded file is empty.";
    public static final String MESSAGE_SCREEN_NAME_REQUIRED = "Parameter 'screenName' is required.";
    public static final String MESSAGE_IMPORT_SCREEN_NOT_CONFIGURED = "Import screen '%s' is not configured.";
    public static final String MESSAGE_IMPORT_COMPLETED = "Import completed successfully.";
    public static final String MESSAGE_PARAMS_INVALID_JSON = "Parameter 'params' must be valid JSON.";
    public static final String MESSAGE_FIELD_REQUIRED = "%s is required";
    public static final String MESSAGE_FILE_TYPE_MUST_CONTAIN_ACTUAL_OR_ESTIMATE =
            "fileType must contain ACTUAL or ESTIMATE: %s";
    public static final String MESSAGE_MISSING_REQUIRED_COLUMNS =
            "The file is missing required column(s): %s.";
    public static final String MESSAGE_TEMPLATE_WORKBOOK_NO_SHEETS = "Template workbook has no sheets";
    public static final String MESSAGE_TEMPLATE_FILE_NOT_FOUND = "Template file not found: %s";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_MISSING =
            "Direct import configuration is missing: classpath resource '%s' was not found.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_INVALID_ROOT =
            "'%s' must be a YAML document with a top-level 'screens' mapping.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_MISSING_SCREENS =
            "'%s' must define a 'screens' object mapping screen names to column lists.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_INVALID_SCREEN_LIST =
            "Invalid direct import config: 'screens.%s' must be a list of column names.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_EMPTY_SCREEN_COLUMNS =
            "Invalid direct import config: 'screens.%s' must list at least one column.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_NO_SCREENS =
            "Invalid direct import config: 'screens' must contain at least one screen definition.";
    public static final String MESSAGE_DIRECT_IMPORT_CONFIG_LOAD_FAILED =
            "Failed to load direct import screen configuration.";
    public static final String MESSAGE_ROW_AT_LEAST_ONE_RATE_REQUIRED =
            "Row %d: At least one rate is required (Rate Type 1 and Value 1).";
    public static final String MESSAGE_ROW_RATE_TYPE_AND_VALUE_REQUIRED =
            "Row %d: Rate Type %d and Value %d must both be provided.";
    public static final String MESSAGE_ROW_MISSING_REQUIRED_VALUE =
            "Row %d: Missing required value: %s.";
    public static final String MESSAGE_ROW_INVALID_NUMERIC_VALUE = "Row %d: Invalid numeric value for Value %d.";
    public static final String MESSAGE_ROW_KEYWORD_MIN_LENGTH =
            "Row %d: Keyword must be at least %d characters.";
    public static final String MESSAGE_ROW_KEYWORD_INVALID_CHARACTERS =
            "Row %d: Keyword must not contain special characters (only letters, numbers, spaces, ., & and @ are allowed).";
    public static final String MESSAGE_ROW_INVALID_MATCH_TYPE =
            "Row %d: Invalid Match Type. Allowed values: " + ALLOWED_MATCH_TYPE_LABELS_TEXT + ".";
    public static final String MESSAGE_FILENAME_EXTENSION_REQUIRED =
            "The uploaded file must have a filename with a .csv or .xlsx extension.";
    public static final String MESSAGE_RATE_SHEET_CONFIG_NO_COLUMNS =
            "RATE_SHEET direct import configuration has no required columns.";
    public static final String MESSAGE_RATE_SHEET_CONFIG_MISSING_COLUMN =
            "RATE_SHEET direct import configuration must list required column '%s' in direct-import-screens.yml.";
    public static final String MESSAGE_RATE_SHEET_ID_REQUIRED = "Parameter 'rateSheetId' is required.";
    public static final String MESSAGE_RATE_SHEET_ID_INVALID_UUID = "Parameter 'rateSheetId' must be a valid UUID.";
    public static final String MESSAGE_WORKBOOK_NO_SHEETS = "The workbook has no sheets.";
    public static final String MESSAGE_SPREADSHEET_HEADER_ROW_REQUIRED =
            "The spreadsheet must contain a header row in the first row.";
    public static final String MESSAGE_CSV_HEADER_ROW_REQUIRED = "The CSV file must contain a header row.";
    public static final String MESSAGE_EXCEL_READ_FAILED = "Could not read the uploaded Excel file.";
    public static final String MESSAGE_CSV_READ_FAILED = "Could not read the uploaded CSV file.";
    public static final String MESSAGE_UNSUPPORTED_FILE_TYPE =
            "Unsupported file type. Upload a .csv or .xlsx file (legacy .xls is not supported).";
    public static final String MESSAGE_MULTIPLE_READERS_MATCHED =
            "Import is misconfigured: more than one spreadsheet reader matched the uploaded file.";
    public static final String MESSAGE_MULTIPLE_PROCESSORS_REGISTERED =
            "Import is misconfigured: more than one ImportProcessor is registered for screen '%s'.";
    public static final String MESSAGE_NO_IMPORT_HANDLER = "No import handler is registered for screen '%s'.";

    public static final List<String> RATE_SHEET_MINIMUM_CONFIGURED_COLUMNS =
            ExportHeaderConstants.RATE_SHEET_MINIMUM_CONFIGURED_COLUMNS;

    public static String normalizeMatchType(String raw) {
        if (raw == null) {
            return "";
        }
        return MATCH_TYPE_NORMALIZE_PATTERN
                .matcher(raw.strip().toUpperCase(Locale.ROOT))
                .replaceAll("");
    }

    public static boolean isAllowedMatchType(String normalized) {
        return normalized != null && ALLOWED_MATCH_TYPES_NORMALIZED.contains(normalized);
    }

    private ImportConstants() {
    }
}
