package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DtaDefaultArAccountResolverTest {

    @Mock
    private RevenueBillingAccountResolver revenueBillingAccountResolver;

    @InjectMocks
    private DtaDefaultArAccountResolver resolver;

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenEntityCodeMissing() {
        assertThat(resolver.resolveOrCreate(" ")).isEmpty();
        verifyNoInteractions(revenueBillingAccountResolver);
    }

    @Test
    void resolveOrCreate_shouldDelegateToRevenueBillingAccountResolver() {
        Account account = new Account();
        when(revenueBillingAccountResolver.resolveBillingAccount("DXB", "DTA", null))
                .thenReturn(Optional.of(account));

        assertThat(resolver.resolveOrCreate(" DXB ")).containsSame(account);
        verify(revenueBillingAccountResolver).resolveBillingAccount("DXB", "DTA", null);
    }
}
