package com.customcharge.persistencelayer.recordtracking;

public enum RecordTrackingEvent {
    FILE_RECEIVED,
    CHUNK_CREATED,
    MESSAGE_SENT_TO_SQS,
    MESSAGE_RECEIVED_FROM_SQS,
    CHUNK_PROCESSING_STARTED,
    CHUNK_PROCESSING_COMPLETED,
    RECORD_PROCESSING_STARTED,
    VALIDATION_STARTED,
    VALIDATION_COMPLETED,
    CALCULATION_STARTED,
    CALCULATION_COMPLETED,
    DB_SAVE_STARTED,
    DB_SAVE_COMPLETED,
    RECORD_COMPLETED,
    RECORD_FAILED,
    ERRORS_PERSISTED,
    FILE_BATCH_COMPLETED,
    FILE_BATCH_FAILED;

    public boolean oncePerChunk() {
        return switch (this) {
            case CHUNK_CREATED, MESSAGE_SENT_TO_SQS, MESSAGE_RECEIVED_FROM_SQS, CHUNK_PROCESSING_STARTED,
                 CHUNK_PROCESSING_COMPLETED, ERRORS_PERSISTED -> true;
            default -> false;
        };
    }

    public boolean oncePerRecord() {
        return switch (this) {
            case RECORD_COMPLETED, RECORD_FAILED -> true;
            default -> false;
        };
    }

    public boolean oncePerFile() {
        return switch (this) {
            case FILE_BATCH_COMPLETED, FILE_BATCH_FAILED -> true;
            default -> false;
        };
    }
}
