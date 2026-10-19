package com.customcharge.persistencelayer.service;

/**
 * Outcome of attempting to claim a chunk for processing.
 */
public enum ChunkBeginResult {
    /** This invocation claimed the chunk and should run processing. */
    ACCEPTED,
    /** Chunk was already completed; safe to acknowledge the queue message. */
    ALREADY_COMPLETED,
    /** Another worker holds an active claim; message must not be acknowledged. */
    IN_PROGRESS
}
