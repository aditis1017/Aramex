package com.customcharge.fileorchestration.service;

import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class EntityCurrencyLookupService {

    private final CurrencyRepository currencyRepository;

    public EntityCurrencyLookupService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> findCurrencyCodeByEntityCode(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return Optional.empty();
        }
        return currencyRepository.findActiveByEntityCode(entityCode.strip())
                .map(Currency::getCurrencyCode)
                .filter(StringUtils::hasText);
    }
}
