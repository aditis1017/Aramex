package com.customcharge.persistencelayer.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountDto extends AbstractDto {
    private String accountNumber;
    private String accountName;
    private String entityCode;
    private AccountNature accountNature;
    private String countryCode;
    private String billingCurrency;
    private String rateCurrency;
    private String accountCategory;
    private String businessType;
    private String defaultCustomsType;
    private String supplierType;
    private String status;
}
