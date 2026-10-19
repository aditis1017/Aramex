package com.customcharge.persistencelayer.accountRedis;

/**
 * Optional hook for callers (e.g. shipment ingest) to skip Redis after failures in a request scope.
 */
public interface AccountRedisReadListener {

    default boolean skipReads() {
        return false;
    }

    default void onReadFailure(String redisKey, Exception ex) {
        // no-op
    }
}
