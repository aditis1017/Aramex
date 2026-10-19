package com.customcharge.chargescalculator.listener;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.service.batch.ChunkProcessingCoordinator;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchOutputQueueListenerTest {

    @Mock
    private ChunkProcessingCoordinator chunkProcessingCoordinator;

    private ObjectMapper objectMapper;

    @InjectMocks
    private BatchOutputQueueListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new BatchOutputQueueListener(objectMapper, chunkProcessingCoordinator);
    }

    @Test
    void listen_shouldIgnoreBlankPayload() throws Exception {
        listener.listen("  ");

        verify(chunkProcessingCoordinator, never()).process(any(ProcessedChunkMessage.class));
    }

    @Test
    void listen_shouldProcessValidPayload() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        String payload = objectMapper.writeValueAsString(message);

        listener.listen(payload);

        verify(chunkProcessingCoordinator).process(any(ProcessedChunkMessage.class));
    }

    @Test
    void listen_shouldAckPoisonMessage_onInvalidJson() throws Exception {
        listener.listen("{not-json");

        verify(chunkProcessingCoordinator, never()).process(any(ProcessedChunkMessage.class));
    }

    @Test
    void listen_shouldRetry_onProcessingFailure() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        String payload = objectMapper.writeValueAsString(message);
        doThrow(new RuntimeException("fail")).when(chunkProcessingCoordinator).process(any(ProcessedChunkMessage.class));

        assertThatThrownBy(() -> listener.listen(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chunk processing failed");
    }
}
