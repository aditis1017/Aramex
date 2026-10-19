package com.customcharge.persistencelayer.accountRedis;

import java.util.Optional;

/**
 * Simple Redis string cache for account payloads (no search / indexing).
 */
public interface AccountRedisStringCache {

    Optional<String> get(String key);

    void put(String key, String value);

    void delete(String key);
}
