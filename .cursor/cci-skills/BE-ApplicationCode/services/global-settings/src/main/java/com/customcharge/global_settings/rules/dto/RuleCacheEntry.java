package com.customcharge.global_settings.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RuleCacheEntry {
    private Integer priority;
    private String rule;
}
