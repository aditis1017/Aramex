package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChunkEnvelopePublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    private ChunkEnvelopePublisher publisher;
    private FileProcessingContext context;

    @BeforeEach
    void setUp() {
        BatchFlowProperties properties = FileOrchestrationTestDataFactory.batchFlowProperties();
        properties.setChunkSize(2);
        properties.setPublishMaxAttempts(1);
        publisher = new ChunkEnvelopePublisher(properties, messagePublisher, new ObjectMapper());
        context = new FileProcessingContext(
                new FileProcessingRequest(
                        "file-123",
                        "user@test.com",
                        "s3://bucket/file.csv",
                        "file.csv",
                        "CSV",
                        "Estimates_Template",
                        "RUH",
                        "trace-1"),
                null);
    }

    @Test
    void publishDataChunk_shouldInjectDefaultCurrency_whenRecordMissingCurrency() throws Exception {
        context = new FileProcessingContext(context.request(), "SAR");

        publisher.publishDataChunk(context, List.of(Map.of("awb", "A1")));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).send(eq("output-queue"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"currency\":\"SAR\"");
    }

    @Test
    void publishDataChunk_shouldSkipEmptyChunk() throws Exception {
        publisher.publishDataChunk(context, List.of());

        verify(messagePublisher, never()).send(eq("output-queue"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void publishDataChunk_shouldPublishWithLastChunkFalse() throws Exception {
        publisher.publishDataChunk(context, List.of(Map.of("awb", "A1")));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).send(eq("output-queue"), payloadCaptor.capture());

        String json = payloadCaptor.getValue();
        assertThat(json).contains("\"chunkIndex\":1");
        assertThat(json).contains("\"lastChunk\":false");
        assertThat(json).contains("\"fileId\":\"file-123\"");
        assertThat(json).contains("\"entityCode\":\"RUH\"");
        assertThat(json).contains("\"fileProcessingStartedAtEpochMs\":");
        assertThat(json).contains("\"uploadedBy\":\"user@test.com\"");
    }

    @Test
    void publishFileCompletionEnvelope_shouldMarkLastChunkWithPublishedCount() throws Exception {
        publisher.publishDataChunk(context, List.of(Map.of("awb", "A1"), Map.of("awb", "A2")));
        publisher.publishDataChunk(context, List.of(Map.of("awb", "A3"), Map.of("awb", "A4")));
        publisher.publishDataChunk(context, List.of(Map.of("awb", "A5")));

        publisher.publishFileCompletionEnvelope(context);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher, org.mockito.Mockito.times(4))
                .send(eq("output-queue"), payloadCaptor.capture());

        String completionJson = payloadCaptor.getAllValues().get(3);
        assertThat(completionJson).contains("\"chunkIndex\":0");
        assertThat(completionJson).contains("\"lastChunk\":true");
        assertThat(completionJson).contains("\"totalChunks\":3");
    }

    @Test
    void enrichRecord_shouldAddCurrency_whenMissingAndDefaultProvided() {
        Map<String, String> enriched = ChunkEnvelopePublisher.enrichRecord(
                new java.util.LinkedHashMap<>(Map.of("awb", "A1")), "SAR");
        assertThat(enriched).containsEntry("currency", "SAR");
    }

    @Test
    void enrichRecord_shouldNotOverwriteExistingCurrency() {
        Map<String, String> enriched = ChunkEnvelopePublisher.enrichRecord(
                new java.util.LinkedHashMap<>(Map.of("awb", "A1", "Currency", "USD")), "SAR");
        assertThat(enriched).containsEntry("Currency", "USD");
        assertThat(enriched).doesNotContainKey("currency");
    }

    @Test
    void publishDataChunk_shouldReturnExistingChunkCount_whenRecordsEmpty() throws Exception {
        context.nextChunkIndex();

        int chunkIndex = publisher.publishDataChunk(context, List.of());

        assertThat(chunkIndex).isEqualTo(1);
        verify(messagePublisher, never()).send(eq("output-queue"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void publishDataChunk_shouldReturnExistingChunkCount_whenRecordsNull() throws Exception {
        context.nextChunkIndex();

        int chunkIndex = publisher.publishDataChunk(context, null);

        assertThat(chunkIndex).isEqualTo(1);
    }

    @Test
    void sendWithRetry_shouldRetryAndEventuallySucceed() throws Exception {
        BatchFlowProperties properties = FileOrchestrationTestDataFactory.batchFlowProperties();
        properties.setPublishMaxAttempts(3);
        properties.setPublishBackoffMillis(1);
        publisher = new ChunkEnvelopePublisher(properties, messagePublisher, new ObjectMapper());

        org.mockito.Mockito.doThrow(new RuntimeException("temporary"))
                .doThrow(new RuntimeException("temporary"))
                .doNothing()
                .when(messagePublisher).send(eq("output-queue"), org.mockito.ArgumentMatchers.anyString());

        publisher.publishDataChunk(context, List.of(Map.of("awb", "A1")));

        verify(messagePublisher, org.mockito.Mockito.times(3))
                .send(eq("output-queue"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void sendWithRetry_shouldFailAfterMaxAttempts() {
        BatchFlowProperties properties = FileOrchestrationTestDataFactory.batchFlowProperties();
        properties.setPublishMaxAttempts(2);
        properties.setPublishBackoffMillis(1);
        publisher = new ChunkEnvelopePublisher(properties, messagePublisher, new ObjectMapper());
        org.mockito.Mockito.doThrow(new RuntimeException("permanent"))
                .when(messagePublisher).send(eq("output-queue"), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> publisher.publishDataChunk(context, List.of(Map.of("awb", "A1"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to publish to output queue after 2 attempts");
    }

    @Test
    void enrichRecords_shouldPreserveOrder() {
        List<Map<String, String>> enriched = ChunkEnvelopePublisher.enrichRecords(
                List.of(Map.of("awb", "A1"), Map.of("awb", "A2")), "SAR");

        assertThat(enriched).hasSize(2);
        assertThat(enriched.get(0)).containsEntry("currency", "SAR");
        assertThat(enriched.get(1)).containsEntry("currency", "SAR");
    }
}
