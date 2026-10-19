package com.customcharge.global_settings.rules.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class AuditHistoryResponseDTO {
    private List<ChangedFieldDTO> items;
    private Integer total;
    private Integer page;
    private Integer size;
}
