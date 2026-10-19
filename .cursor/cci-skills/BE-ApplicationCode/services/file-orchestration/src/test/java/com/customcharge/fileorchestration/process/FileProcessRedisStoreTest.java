package com.customcharge.fileorchestration.process;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileProcessRedisStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private BatchFlowProperties properties;
    private FileProcessRedisStore store;

    @BeforeEach
    void setUp() {
        properties = new BatchFlowProperties();
        properties.setRedisTtlSeconds(1800);
        properties.setHeartbeatEveryChunks(2);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(redis.opsForHash()).thenReturn(hashOps);
        store = new FileProcessRedisStore(redis, properties);
    }

    @Test
    void tryAcquire_shouldReturnTrue_andInitProgress_whenLockAcquired() {
        when(valueOps.setIfAbsent(eq(FileProcessRedisStore.lockKey("f1")), anyString(), any(Duration.class)))
                .thenReturn(true);

        boolean acquired = store.tryAcquire(request("f1"));

        assertThat(acquired).isTrue();
        verify(hashOps).putAll(eq(FileProcessRedisStore.progressKey("f1")), anyMap());
        verify(redis).expire(eq(FileProcessRedisStore.progressKey("f1")), eq(Duration.ofSeconds(1800)));
    }

    @Test
    void tryAcquire_shouldReturnFalse_whenLockHeld() {
        when(valueOps.setIfAbsent(eq(FileProcessRedisStore.lockKey("f1")), anyString(), any(Duration.class)))
                .thenReturn(false);

        boolean acquired = store.tryAcquire(request("f1"));

        assertThat(acquired).isFalse();
        verify(hashOps, never()).putAll(anyString(), anyMap());
    }

    @Test
    void tryAcquire_shouldTolerateNullOptionalFields() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        boolean acquired = store.tryAcquire(new FileProcessingRequest(
                "f1", null, null, null, null, null, null, null));

        assertThat(acquired).isTrue();
        verify(hashOps).putAll(eq(FileProcessRedisStore.progressKey("f1")), anyMap());
    }

    @Test
    void markStatus_shouldUpdateHashAndRefreshTtl() {
        store.markStatus("f1", FileProcessRedisStore.STATUS_FETCHING);

        verify(hashOps).put(FileProcessRedisStore.progressKey("f1"), "status", FileProcessRedisStore.STATUS_FETCHING);
        verify(hashOps).put(eq(FileProcessRedisStore.progressKey("f1")), eq("updatedAt"), anyString());
        verify(redis, times(2)).expire(anyString(), eq(Duration.ofSeconds(1800)));
    }

    @Test
    void markStatus_shouldSwallowRedisFailures() {
        doThrow(new RuntimeException("redis down"))
                .when(hashOps).put(anyString(), anyString(), anyString());

        store.markStatus("f1", FileProcessRedisStore.STATUS_FAILED);
    }

    @Test
    void markDedupComplete_shouldPublishPublishingStatusAndRowCount() {
        store.markDedupComplete("f1", 42);

        org.mockito.ArgumentCaptor<Map<String, String>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(eq(FileProcessRedisStore.progressKey("f1")), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("status", FileProcessRedisStore.STATUS_PUBLISHING)
                .containsEntry("totalRows", "42");
        assertThat(captor.getValue().get("updatedAt")).isNotBlank();
    }

    @Test
    void onChunkPublished_shouldIncrementRecordsAndRefreshTtlOnHeartbeat() {
        store.onChunkPublished("f1", 2, 5, 2);

        verify(hashOps).put(FileProcessRedisStore.progressKey("f1"), "lastChunkIndex", "2");
        verify(hashOps).put(FileProcessRedisStore.progressKey("f1"), "chunksPublished", "2");
        verify(hashOps).increment(FileProcessRedisStore.progressKey("f1"), "recordsPublished", 5);
        verify(redis, times(2)).expire(anyString(), eq(Duration.ofSeconds(1800)));
    }

    @Test
    void onChunkPublished_shouldSkipTtlRefreshBetweenHeartbeats() {
        properties.setHeartbeatEveryChunks(5);

        store.onChunkPublished("f1", 1, 3, 1);

        verify(hashOps).increment(FileProcessRedisStore.progressKey("f1"), "recordsPublished", 3);
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void markFailed_shouldTruncateLongErrorMessage() {
        store.markFailed("f1", "x".repeat(600));

        verify(hashOps).putAll(eq(FileProcessRedisStore.progressKey("f1")), org.mockito.ArgumentMatchers.<Map<String, String>>argThat(
                fields -> fields.get("status").equals(FileProcessRedisStore.STATUS_FAILED)
                        && fields.get("error").length() == 500));
    }

    @Test
    void markFailed_shouldHandleNullMessage() {
        store.markFailed("f1", null);

        verify(hashOps).putAll(eq(FileProcessRedisStore.progressKey("f1")), org.mockito.ArgumentMatchers.<Map<String, String>>argThat(
                fields -> fields.get("error").isEmpty()));
    }

    @Test
    void release_shouldDeleteLockAndProgressKeys() {
        store.release("f1");

        verify(redis).delete(FileProcessRedisStore.lockKey("f1"));
        verify(redis).delete(FileProcessRedisStore.progressKey("f1"));
    }

    @Test
    void release_shouldSwallowRedisFailures() {
        doThrow(new RuntimeException("delete failed")).when(redis).delete(anyString());

        store.release("f1");
    }

    @Test
    void lockAndProgressKeys_shouldUseFileIdPrefix() {
        assertThat(FileProcessRedisStore.lockKey("abc")).isEqualTo("fo:lock:abc");
        assertThat(FileProcessRedisStore.progressKey("abc")).isEqualTo("fo:progress:abc");
    }

    private static FileProcessingRequest request(String fileId) {
        return new FileProcessingRequest(
                fileId, "user", "path", "file.csv", "CSV", "Estimates_Template", "RUH", "trace");
    }
}
