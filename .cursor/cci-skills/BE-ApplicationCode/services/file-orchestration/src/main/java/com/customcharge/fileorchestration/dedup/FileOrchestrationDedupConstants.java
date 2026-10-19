package com.customcharge.fileorchestration.dedup;

public final class FileOrchestrationDedupConstants {

    public static final String PROCESS_FOR_ESTIMATE = "Estimates_Template";
    public static final String PROCESS_FOR_ACTUAL = "Actual_Template";

    public static final String COLUMN_AWB = "awb";  // TODO Will Update based on Entity after Entity Configuration
    public static final String PAYMENT_REFERENCE = "saddad"; // TODO Will Update based on Entity after Entity Configuration
    public static final String DECLARATION_NUMBER = "declaration number";

    static final String MISSING_AWB_KEY = "__missing_awb__";
    static final String KEY_SEPARATOR = "|";

    public static final int DEDUP_ERROR_CHUNK_NUMBER = 0;
    public static final String DUPLICATE_AWB_ERROR_MESSAGE =
            "Remove this row from process because of duplicate AWB";
    public static final String DUPLICATE_AWB_SADDAD_ERROR_MESSAGE =
            "Remove this row from process because of duplicate AWB and Saddad";
    public static final String DUPLICATE_DECLARATION_NUMBER_ERROR_MESSAGE =
            "Remove this row from process because of duplicate Declaration Number";

    private FileOrchestrationDedupConstants() {
    }
}
