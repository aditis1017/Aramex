package com.customcharge.global_settings.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UpdateGlobalSettingSequenceRequest {
    private String targetValue;
    private List<String> orderedRowIds;
}
