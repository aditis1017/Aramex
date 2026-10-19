package com.customcharge.persistencelayer.accountRedis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArAccountRedisServiceCharge {

    private UUID id;
    private UUID serviceCodeMasterId;
    private String serviceCode;
    private String description;
    private String serviceDescription;
    private String type;
    private Boolean masterActive;
    private Double taxValue;
    private String taxCode;
    private Boolean active;
    private String defaultAccountNumber;
    private Boolean isBillable;
    private String split;
    private String infoAxsId;
    private Boolean fetchTax;
}
