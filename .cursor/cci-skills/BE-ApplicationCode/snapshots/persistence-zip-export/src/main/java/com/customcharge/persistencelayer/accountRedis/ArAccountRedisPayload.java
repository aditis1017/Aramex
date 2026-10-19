package com.customcharge.persistencelayer.accountRedis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArAccountRedisPayload {

    private UUID id;
    private String accountNo;
    private String country;
    private String accountName;
    /** Populated for entity-settings aggregate (key {@code acc:entityCode}). */
    private String entity;
    private String currency;
    private String arAccountNo;
    private String marketStructure;
    private String accountType;
    private String status;
    private String businessType;
    private String businessSegment;
    private String rateCurrency;
    private String billingCurrency;
    /** AR master customs type (master category CustomsType). */
    private String customType;
    private String teamId;
    private String teamName;
    private String reachAccountId;
    private Instant createdDate;
    private String createdBy;
    private Map<String, ArAccountRedisChargeTypeData> charges = new LinkedHashMap<>();
}
