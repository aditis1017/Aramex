package com.customcharge.chargescalculator.controller;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.listener.BatchOutputQueueListener;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchOutputChunkControllerTest {

    @Mock
    private BatchOutputQueueListener batchOutputQueueListener;

    private BatchOutputChunkController controller;

    @BeforeEach
    void setUp() {
        controller = new BatchOutputChunkController(batchOutputQueueListener, new ObjectMapper());
    }

    @Test
    void ingestChunk_shouldReturnAccepted() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();

        ResponseEntity<Void> response = controller.ingestChunk(message);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(batchOutputQueueListener).listen(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void ingestChunk_shouldReturn500_whenListenerFails() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        doThrow(new RuntimeException("boom")).when(batchOutputQueueListener).listen(org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> controller.ingestChunk(message))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
