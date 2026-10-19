package com.customcharge.chargescalculator.service.batch;

import java.util.UUID;

/**
 * Thrown when a chunk is already being processed elsewhere so the SQS message
 * is not acknowledged and will be retried after visibility timeout.
 */
public class ChunkInProgressException extends RuntimeException {

    private final UUID fileId;
    private final int chunkIndex;

    public ChunkInProgressException(UUID fileId, int chunkIndex) {
        super("Chunk in progress elsewhere fileId=" + fileId + " chunkIndex=" + chunkIndex);
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
    }

    public UUID getFileId() {
        return fileId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }
}
