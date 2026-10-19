package com.customcharge.fileorchestration.dto.s3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3EventRecord {

    private String eventName;
    private S3EventDetail s3;
}
