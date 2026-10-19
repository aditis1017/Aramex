package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateConfigResponse {

    private UUID rateId;
    private RateType rateType;
    private BigDecimal value;
    private PercentageOf percentageOf;
    private String currency;

    private Map<String, List<String>> dropdowns;
}