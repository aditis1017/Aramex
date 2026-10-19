package com.customcharge.fileorchestration.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ToString
public class ProcessedChunkMessage {
    private String uploadedBy;
    private LocalDateTime uploadTime;
    private String documentPath;
    private String fileName;
    private String fileId;
    private String fileType;
    private int chunkIndex;
    private int recordCount;
    private String traceId;
    private String processFor;
    private String entityCode;
    private Integer totalChunks;
    private Boolean lastChunk;
    private Long fileProcessingStartedAtEpochMs;
    private List<Map<String, String>> records;
}
