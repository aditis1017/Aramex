package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountCurrencyResolverTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private AccountCurrencyResolver resolver;

    @Test
    void resolveFromAccount_shouldReturnNull_whenAccountMissing() {
        assertThat(resolver.resolveFromAccount(null)).isNull();
        verifyNoInteractions(currencyRepository);
    }

    @Test
    void resolveFromAccount_shouldReturnNull_whenEntityCodeMissing() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

        assertThat(resolver.resolveFromAccount(account)).isNull();
        verifyNoInteractions(currencyRepository);
    }

    @Test
    void resolveFromAccount_shouldResolveFromEntityCodeField() {
        Entity entity = new Entity();
        entity.setCode(" RUH ");
        Account account = new Account();
        account.setEntity(entity);
        Currency currency = new Currency();
        currency.setCurrencyCode("sar");
        when(currencyRepository.findActiveByEntityCode("RUH")).thenReturn(Optional.of(currency));

        assertThat(resolver.resolveFromAccount(account)).isEqualTo("SAR");
    }

    @Test
    void resolveFromAccount_shouldResolveFromEntityRelation() {
        Entity entity = new Entity();
        entity.setCode("JED");
        Account account = new Account();
        account.setEntity(entity);
        Currency currency = new Currency();
        currency.setCurrencyCode("SAR");
        when(currencyRepository.findActiveByEntityCode("JED")).thenReturn(Optional.of(currency));

        assertThat(resolver.resolveFromAccount(account)).isEqualTo("SAR");
    }

    @Test
    void resolveFromAccount_shouldReturnNull_whenNoActiveCurrencyMapping() {
        Entity entity = new Entity();
        entity.setCode("DXB");
        Account account = new Account();
        account.setEntity(entity);
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.empty());

        assertThat(resolver.resolveFromAccount(account)).isNull();
    }
}
