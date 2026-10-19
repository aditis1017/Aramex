package com.customcharge.dashboard.constants;

import java.util.List;

/**
 * Audit export pipeline constants.
 */
public final class AuditExportConstants {

    public static final String EXPORT_TYPE_SHIPMENT_AUDIT = "SHIPMENT_AUDIT";
    public static final String EXPORT_TYPE_ACCOUNT_AUDIT = "ACCOUNT_AUDIT";

    public static final String FILE_NAME_PREFIX_SHIPMENT = "Shipment-Audit-History";
    public static final String FILE_NAME_PREFIX_ACCOUNT = "Account-Audit-History";

    public static final String COLUMN_TIMESTAMP = "Timestamp";
    public static final String COLUMN_SECTION = "Section";
    public static final String COLUMN_USER = "User";
    public static final String COLUMN_ACTION = "Action";
    public static final String COLUMN_FIELD_NAME = "Field Name";
    public static final String COLUMN_OLD_VALUE = "Old Value";
    public static final String COLUMN_NEW_VALUE = "New Value";
    public static final String COLUMN_TRACKING_NUMBER = "Tracking Number";

    public static final List<String> COLUMN_NAMES = List.of(
            COLUMN_TIMESTAMP,
            COLUMN_SECTION,
            COLUMN_USER,
            COLUMN_ACTION,
            COLUMN_FIELD_NAME,
            COLUMN_OLD_VALUE,
            COLUMN_NEW_VALUE);

    public static final List<String> SHIPMENT_COLUMN_NAMES = List.of(
            COLUMN_TRACKING_NUMBER,
            COLUMN_TIMESTAMP,
            COLUMN_SECTION,
            COLUMN_USER,
            COLUMN_ACTION,
            COLUMN_FIELD_NAME,
            COLUMN_OLD_VALUE,
            COLUMN_NEW_VALUE);

    public static final String FILTER_KEY_FROM_DATE = "fromDate";
    public static final String FILTER_KEY_TO_DATE = "toDate";
    public static final String FILTER_KEY_HAWB_NUMBER = "hawbNumber";
    public static final String FILTER_KEY_ACCOUNT_ID = "accountId";
    public static final String FILTER_KEY_ENTITY_CODE = "entityCode";
    public static final String FILTER_KEY_MANAGED_TYPE = "managedType";

    public static final String FILTER_SPEC_REQUIRED = "filterSpec is required (fromDate, toDate, hawbNumber)";
    public static final String FILTER_SPEC_REQUIRED_ACCOUNT =
            "filterSpec is required (fromDate, toDate, accountId and/or entityCode)";
    public static final String FILTER_SPEC_HAWB_NUMBER_REQUIRED = "filterSpec.hawbNumber is required";
    public static final String FILTER_SPEC_FIELD_REQUIRED_PREFIX = "filterSpec.";

    public static final String AUTHOR_UNKNOWN = "unknown";
    public static final String AUTHOR_SYSTEM = "System";

    public static final String OPERATION_INITIAL = "INITIAL";
    public static final String OPERATION_CREATE = "Create";

    private AuditExportConstants() {
    }
}
