package com.customcharge.persistencelayer.accountRedis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArAccountRedisRateSheetItem {

    private UUID id;
    private String description;
    private String serviceCode;
    private String shipmentType;
    private String businessType;
    private String categoryType;
    private List<ArAccountRedisRateSheetItemRate> rates = new ArrayList<>();
}
