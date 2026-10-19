package com.customcharge.persistencelayer.dto;

import com.customcharge.persistencelayer.entities.Account;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
public class ServiceChargeQueryCriteria {

    private final String entityCode;
    private final Account account;
    private final List<String> chargeTypes;
    private final Boolean active;

    public static ServiceChargeQueryCriteria of(String entityCode) {
        return ServiceChargeQueryCriteria.builder().entityCode(entityCode).build();
    }
}
