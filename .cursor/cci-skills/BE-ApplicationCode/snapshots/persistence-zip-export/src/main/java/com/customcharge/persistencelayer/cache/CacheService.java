package com.customcharge.persistencelayer.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> Optional<List<T>> getList(String key, Class<T> elementType);

    <T> void put(String key, T value, Duration ttl);

    void evict(String key);

    public void deleteCacheUsingPattern(String pattern);
}