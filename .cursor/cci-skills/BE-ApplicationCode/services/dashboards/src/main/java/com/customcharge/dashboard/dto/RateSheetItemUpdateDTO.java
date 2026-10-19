package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateSheetItemUpdateDTO {
    private UUID id;
    private String description;
    private String serviceCode;
    private String shipmentType;
    private String businessType;
    private String categoryType;
    private List<RateSheetItemRateResponse> rates;
    private Map<String, Object> dropdowns;
}
