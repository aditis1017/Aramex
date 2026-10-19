package com.customcharge.dashboard.service;

import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceChargeRateSheetUsageValidatorTest {

    @Mock
    private RateSheetRepository rateSheetRepository;
    @Mock
    private RateSheetItemRepository rateSheetItemRepository;

    @InjectMocks
    private ServiceChargeRateSheetUsageValidator validator;

    @Test
    void assertNotUsedOnRateSheet_shouldIgnoreNullAccountId() {
        validator.assertNotUsedOnRateSheet(null, "SC01");
        verifyNoInteractions(rateSheetRepository, rateSheetItemRepository);
    }

    @Test
    void assertNotUsedOnRateSheet_shouldIgnoreBlankServiceCode() {
        validator.assertNotUsedOnRateSheet(UUID.randomUUID(), " ");
        verifyNoInteractions(rateSheetRepository, rateSheetItemRepository);
    }

    @Test
    void assertNotUsedOnRateSheet_shouldIgnoreWhenRateSheetMissing() {
        UUID accountId = UUID.randomUUID();
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());

        assertThatCode(() -> validator.assertNotUsedOnRateSheet(accountId, "SC01"))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNotUsedOnRateSheet_shouldRejectWhenServiceCodeInUse() {
        UUID accountId = UUID.randomUUID();
        UUID rateSheetId = UUID.randomUUID();

        RateSheet rateSheet = new RateSheet();
        ReflectionTestUtils.setField(rateSheet, "id", rateSheetId);

        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("SC01");

        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(rateSheet));
        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(item));

        assertThatThrownBy(() -> validator.assertNotUsedOnRateSheet(accountId, " sc01 "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sc01");
    }

    @Test
    void assertNotUsedOnRateSheet_shouldAllowWhenServiceCodeNotReferenced() {
        UUID accountId = UUID.randomUUID();
        UUID rateSheetId = UUID.randomUUID();

        RateSheet rateSheet = new RateSheet();
        ReflectionTestUtils.setField(rateSheet, "id", rateSheetId);

        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("OTHER");

        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(rateSheet));
        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(item));

        assertThatCode(() -> validator.assertNotUsedOnRateSheet(accountId, "SC01"))
                .doesNotThrowAnyException();
    }
}
