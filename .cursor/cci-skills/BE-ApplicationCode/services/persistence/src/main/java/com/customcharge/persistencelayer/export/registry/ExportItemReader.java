package com.customcharge.persistencelayer.export.registry;

/**
 * Streams export source items one at a time (paginated audit rows, etc.).
 */
public interface ExportItemReader extends AutoCloseable {

    Object read() throws Exception;

    @Override
    default void close() {
        // no-op
    }
}
