package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.customcharge.shipment.constants.LogMessages.ACCOUNT_CURRENCY_NO_ACTIVE_MAPPING;
import static com.customcharge.shipment.constants.LogMessages.ACCOUNT_CURRENCY_NO_ENTITY;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AccountCurrencyResolver {

    CurrencyRepository currencyRepository;

    /**
     * Currency for the account's entity country (rate-sheet / billing account), not charge destination.
     */
    public String resolveFromAccount(Account account) {
        if (account == null) {
            return null;
        }
        String entityCode = resolveEntityCode(account);
        if (!StringUtils.hasText(entityCode)) {
            log.warn(ACCOUNT_CURRENCY_NO_ENTITY, account.getId());
            return null;
        }
        return currencyRepository
                .findActiveByEntityCode(entityCode.strip())
                .map(Currency::getCurrencyCode)
                .map(ExchangeRateService::normalizeCurrency)
                .orElseGet(() -> {
                    log.warn(
                            ACCOUNT_CURRENCY_NO_ACTIVE_MAPPING,
                            entityCode,
                            account.getId());
                    return null;
                });
    }

    private static String resolveEntityCode(Account account) {
        if (StringUtils.hasText(account.getEntityCode())) {
            return account.getEntityCode().strip();
        }
        if (account.getEntity() != null && StringUtils.hasText(account.getEntity().getCode())) {
            return account.getEntity().getCode().strip();
        }
        return null;
    }
}
