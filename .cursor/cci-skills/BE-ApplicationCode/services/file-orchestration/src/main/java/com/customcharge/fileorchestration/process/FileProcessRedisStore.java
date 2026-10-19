package com.customcharge.fileorchestration.process;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class FileProcessRedisStore {

    private static final Logger log = LoggerFactory.getLogger(FileProcessRedisStore.class);

    public static final String STATUS_CLAIMED = "CLAIMED";
    public static final String STATUS_FETCHING = "FETCHING";
    public static final String STATUS_DEDUPING = "DEDUPING";
    public static final String STATUS_PUBLISHING = "PUBLISHING";
    public static final String STATUS_FINALIZING = "FINALIZING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private final StringRedisTemplate redis;
    private final BatchFlowProperties properties;
    private final String podName;

    public FileProcessRedisStore(StringRedisTemplate redis, BatchFlowProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.podName = resolvePodName();
    }

    /**
     * Acquires cross-pod lock. Returns {@code false} if another worker owns the file.
     * Throws if Redis is unavailable (fail closed).
     */
    public boolean tryAcquire(FileProcessingRequest request) {
        String lockKey = lockKey(request.fileId());
        String value = podName + "|" + request.traceId() + "|" + Instant.now().toEpochMilli();
        Boolean acquired = redis.opsForValue().setIfAbsent(
                lockKey, value, Duration.ofSeconds(properties.getRedisTtlSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Redis lock not acquired fileId={} (already processing)", request.fileId());
            return false;
        }
        upsertProgress(request, STATUS_CLAIMED, Map.of(
                "fileName", nullToEmpty(request.fileName()),
                "fileType", nullToEmpty(request.fileType()),
                "processFor", nullToEmpty(request.processFor()),
                "traceId", nullToEmpty(request.traceId()),
                "pod", podName,
                "startedAt", Instant.now().toString(),
                "chunksPublished", "0",
                "recordsPublished", "0",
                "lastChunkIndex", "0"));
        return true;
    }

    public void markStatus(String fileId, String status) {
        try {
            redis.opsForHash().put(progressKey(fileId), "status", status);
            redis.opsForHash().put(progressKey(fileId), "updatedAt", Instant.now().toString());
            touchTtl(fileId);
        } catch (RuntimeException ex) {
            log.warn("Failed to update Redis status fileId={} status={}: {}", fileId, status, ex.getMessage());
        }
    }

    public void markDedupComplete(String fileId, int totalRows) {
        try {
            Map<String, String> fields = new HashMap<>();
            fields.put("status", STATUS_PUBLISHING);
            fields.put("totalRows", Integer.toString(totalRows));
            fields.put("updatedAt", Instant.now().toString());
            redis.opsForHash().putAll(progressKey(fileId), fields);
            touchTtl(fileId);
        } catch (RuntimeException ex) {
            log.warn("Failed to update Redis dedup progress fileId={}: {}", fileId, ex.getMessage());
        }
    }

    public void onChunkPublished(String fileId, int chunkIndex, int recordCount, int chunksPublished) {
        try {
            String key = progressKey(fileId);
            redis.opsForHash().put(key, "lastChunkIndex", Integer.toString(chunkIndex));
            redis.opsForHash().put(key, "chunksPublished", Integer.toString(chunksPublished));
            redis.opsForHash().increment(key, "recordsPublished", recordCount);
            redis.opsForHash().put(key, "updatedAt", Instant.now().toString());
            if (chunksPublished % properties.getHeartbeatEveryChunks() == 0) {
                touchTtl(fileId);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to update Redis chunk progress fileId={} chunkIndex={}: {}",
                    fileId, chunkIndex, ex.getMessage());
        }
    }

    public void markFailed(String fileId, String errorMessage) {
        try {
            Map<String, String> fields = new HashMap<>();
            fields.put("status", STATUS_FAILED);
            fields.put("error", truncate(errorMessage, 500));
            fields.put("updatedAt", Instant.now().toString());
            redis.opsForHash().putAll(progressKey(fileId), fields);
        } catch (RuntimeException ex) {
            log.warn("Failed to mark Redis FAILED fileId={}: {}", fileId, ex.getMessage());
        }
    }

    public void release(String fileId) {
        try {
            redis.delete(lockKey(fileId));
            redis.delete(progressKey(fileId));
            log.info("Released Redis process keys fileId={}", fileId);
        } catch (RuntimeException ex) {
            log.error("Failed to release Redis process keys fileId={}: {}", fileId, ex.getMessage());
        }
    }

    static String lockKey(String fileId) {
        return "fo:lock:" + fileId;
    }

    static String progressKey(String fileId) {
        return "fo:progress:" + fileId;
    }

    private void upsertProgress(FileProcessingRequest request, String status, Map<String, String> extra) {
        try {
            Map<String, String> fields = new HashMap<>(extra);
            fields.put("status", status);
            fields.put("updatedAt", Instant.now().toString());
            String key = progressKey(request.fileId());
            redis.opsForHash().putAll(key, fields);
            redis.expire(key, Duration.ofSeconds(properties.getRedisTtlSeconds()));
        } catch (RuntimeException ex) {
            log.warn("Failed to init Redis progress fileId={}: {}", request.fileId(), ex.getMessage());
        }
    }

    private void touchTtl(String fileId) {
        Duration ttl = Duration.ofSeconds(properties.getRedisTtlSeconds());
        redis.expire(lockKey(fileId), ttl);
        redis.expire(progressKey(fileId), ttl);
    }

    private static String resolvePodName() {
        try {
            String env = System.getenv("HOSTNAME");
            if (env != null && !env.isBlank()) {
                return env;
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
