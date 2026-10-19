package com.customcharge.dashboard.constants;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Excel export formatting constants.
 */
public final class ExportConstants {

    public static final DateTimeFormatter EXPORT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public static final DateTimeFormatter EXPORT_FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    public static final DateTimeFormatter EXPORT_FILE_TIMESTAMP_LOCAL =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static final String MEDIA_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String FILE_EXTENSION_XLSX = ".xlsx";

    public static final String HTTP_CONNECTION_CLOSE = "close";

    public static final String RATE_SHEET_EMPTY_CELL = "-";
    public static final int RATE_SHEET_RATE_SLOTS = 3;
    public static final String REVENUE_EXPORT_SPLIT = "1";

    public static final String EXPORT_VALUE_YES = "Yes";
    public static final String EXPORT_VALUE_NO = "No";

    public static final String FILE_NAME_PREFIX_ROLE = "Role";
    public static final String FILE_NAME_PREFIX_SERVICE_CODE = "ServiceCode";
    public static final String FILE_NAME_PREFIX_RATE_SHEET = "RateSheet";
    public static final String FILE_NAME_PREFIX_RATE_SHEET_IMPORT_TEMPLATE = "RateSheetImportTemplate";
    public static final String FILE_NAME_PREFIX_ENTITY_CHARGES = "EntityCharges";

    private ExportConstants() {
    }
}
