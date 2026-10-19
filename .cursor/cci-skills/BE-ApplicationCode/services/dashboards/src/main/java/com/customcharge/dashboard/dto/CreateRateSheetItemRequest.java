package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateRateSheetItemRequest {
    private UUID rateSheetId;
    private AccountNature accountNature;
    private String accountNumber;
    private String entity;
    private String chargeType;
    private UUID rateSheetItemId;
    @NotBlank
    private String description;
    @NotBlank
    private String serviceCode;
    @NotBlank
    private String shipmentType;
    @NotBlank
    private String businessType;
    @NotBlank
    private String categoryType;
    @NotEmpty
    @Valid
    private List<RateSheetItemRateRequest> rates;
}
