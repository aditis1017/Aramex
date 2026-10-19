package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordTrackingMessagePublisherDecoratorTest {

    @Mock
    private MessagePublisher delegate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        MDC.clear();
        RecordTrackingTimeline.clearAll();
    }

    @Test
    void send_shouldDelegateNonStringPayloadWithoutMdc() {
        RecordTrackingMessagePublisherDecorator decorator =
                new RecordTrackingMessagePublisherDecorator(delegate, objectMapper, "test-service");

        decorator.send("queue", 42);

        verify(delegate).send("queue", 42);
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isNull();
    }

    @Test
    void send_shouldPopulateMdcForTrackableChunk() throws Exception {
        RecordTrackingMessagePublisherDecorator decorator =
                new RecordTrackingMessagePublisherDecorator(delegate, objectMapper, "test-service");
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "traceId", "trace-1",
                "fileId", "file-1",
                "fileName", "batch.csv",
                "processFor", "template",
                "uploadedBy", "tester",
                "entityCode", "DXB",
                "chunkIndex", 2,
                "recordCount", 3,
                "records", java.util.List.of(
                        java.util.Map.of("awb", "A1"),
                        java.util.Map.of("awb", "A2"),
                        java.util.Map.of("awb", "A3"))));

        decorator.send("queue", payload);

        verify(delegate).send("queue", payload);
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("trace-1");
        assertThat(MDC.get(RecordTrackingMdc.FILE_ID)).isEqualTo("file-1");
        assertThat(MDC.get(RecordTrackingMdc.SERVICE_NAME)).isEqualTo("test-service");
        assertThat(MDC.get(RecordTrackingTimeline.CHUNK_STARTED_AT)).isNotBlank();
    }

    @Test
    void send_shouldSkipMdcForLastChunkEnvelope() throws Exception {
        RecordTrackingMessagePublisherDecorator decorator =
                new RecordTrackingMessagePublisherDecorator(delegate, objectMapper, "test-service");
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "traceId", "trace-1",
                "fileId", "file-1",
                "lastChunk", true,
                "chunkIndex", 1,
                "recordCount", 0,
                "records", java.util.List.of()));

        decorator.send("queue", payload);

        verify(delegate).send("queue", payload);
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isNull();
    }

    @Test
    void send_shouldSkipMdcForMalformedJson() {
        RecordTrackingMessagePublisherDecorator decorator =
                new RecordTrackingMessagePublisherDecorator(delegate, objectMapper, "test-service");

        decorator.send("queue", "{not-json");

        verify(delegate).send("queue", "{not-json");
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isNull();
    }
}
