package com.customcharge.dashboard.constants;

/**
 * S3 presigned file upload constants.
 */
public final class FileUploadConstants {

    public static final int DEFAULT_PRESIGN_TTL_SECONDS = 120;
    public static final String DEFAULT_INCOMING_UPLOAD_KEY_PREFIX = "uploaded-files";

    public static final String EXTENSION_CSV = ".csv";
    public static final String EXTENSION_XLSX = ".xlsx";
    public static final String EXTENSION_XLS = ".xls";

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public static final String MESSAGE_FILE_ID_EXISTS = "fileId already exists";
    public static final String MESSAGE_INVALID_FILE_EXTENSION = "Use .csv or .xlsx file only";
    public static final String MESSAGE_BUCKET_MISSING = "Bucket name is missing";
    public static final String MESSAGE_INVALID_S3_KEY = "Invalid s3Key";
    public static final String MESSAGE_FILE_NAME_REQUIRED = "fileName is required";
    public static final String MESSAGE_INVALID_FILE_NAME = "Invalid file name";
    public static final String MESSAGE_S3_PRESIGN_NOT_CONFIGURED = "S3 presign not configured";

    public static final String PATH_TRAVERSAL_SEQUENCE = "..";
    public static final String KEY_PREFIX_TRIM_PATTERN = "^/+|/+$";
    public static final String FILE_NAME_SANITIZE_PATTERN = "[^a-zA-Z0-9._-]";
    public static final int MAX_FILE_NAME_LENGTH = 200;
    public static final int MIN_PRESIGN_TTL_SECONDS = 60;

    private FileUploadConstants() {
    }
}
