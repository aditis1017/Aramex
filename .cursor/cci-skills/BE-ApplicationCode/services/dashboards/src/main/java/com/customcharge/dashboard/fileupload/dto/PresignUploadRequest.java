package com.customcharge.dashboard.fileupload.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PresignUploadRequest {
    private UUID fileId;
    @NotBlank
    private String fileName;
    private String contentType;
    private String entityName;
    @NotBlank
    private String fileFor;
}
