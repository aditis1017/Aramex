package com.customcharge.global_settings.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangedFieldDTO {
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String operation;
}
