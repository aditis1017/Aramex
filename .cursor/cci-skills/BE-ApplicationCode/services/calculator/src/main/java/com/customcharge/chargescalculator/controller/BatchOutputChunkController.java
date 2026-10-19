package com.customcharge.chargescalculator.controller;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.listener.BatchOutputQueueListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/batch-output")
public class BatchOutputChunkController {

    private final BatchOutputQueueListener batchOutputQueueListener;
    private final ObjectMapper objectMapper;

    public BatchOutputChunkController(BatchOutputQueueListener batchOutputQueueListener, ObjectMapper objectMapper) {
        this.batchOutputQueueListener = batchOutputQueueListener;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/chunks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> ingestChunk(@RequestBody ProcessedChunkMessage message) {
        try {
            batchOutputQueueListener.listen(objectMapper.writeValueAsString(message));
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chunk processing failed", ex);
        }
    }
}
