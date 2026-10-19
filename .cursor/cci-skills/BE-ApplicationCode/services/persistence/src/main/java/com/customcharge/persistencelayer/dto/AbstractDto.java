package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public abstract class AbstractDto implements Serializable {
    private UUID id;
    private String createdBy;
    private String updatedBy;
    private Instant createdTime;
    private Instant updatedTime;
}
