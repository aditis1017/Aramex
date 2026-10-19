package com.customcharge.dashboard.service;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetItemDuplicateValidatorTest {

    @Mock
    private RateSheetItemRepository rateSheetItemRepository;

    @InjectMocks
    private RateSheetItemDuplicateValidator validator;

    @Test
    void assertNotDuplicateOnCreate_shouldRejectExistingItem() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("EXPRESS");
        existing.setShipmentType("DDU");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "express", "DDU", "B2B", "LV"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldAllowSameServiceCodeWithDifferentBusinessType() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("DDU");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatCode(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "duty", "DDU", "B2C", "LV"))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNoDuplicatesInBatch_shouldRejectDuplicateRows() {
        var item = new RateSheetItemDuplicateValidator.ItemKey("A", "DDU", "B2B", "LV");

        assertThatThrownBy(() -> validator.assertNoDuplicatesInBatch(List.of(item, item)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void assertNoDuplicatesInBatch_shouldAllowSameServiceCodeWithDifferentShipmentType() {
        var ddu = new RateSheetItemDuplicateValidator.ItemKey("A", "DDU", "B2B", "LV");
        var ddp = new RateSheetItemDuplicateValidator.ItemKey("A", "DDP", "B2B", "LV");

        assertThatCode(() -> validator.assertNoDuplicatesInBatch(List.of(ddu, ddp)))
                .doesNotThrowAnyException();
    }

    @Test
    void assertImportNotDuplicate_shouldRejectDuplicatesInFile() {
        UUID rateSheetId = UUID.randomUUID();
        var item = new RateSheetItemDuplicateValidator.ItemKey("A", "DDU", "ALL", "LV");

        assertThatThrownBy(() -> validator.assertImportNotDuplicate(rateSheetId, List.of(item, item)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Import rejected");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldPassWhenUnique() {
        UUID rateSheetId = UUID.randomUUID();
        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of());

        assertThatCode(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "A", "DDU", "B2B", "LV"))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNotDuplicateOnUpdate_shouldRejectExistingItem() {
        UUID rateSheetId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("A");
        existing.setShipmentType("DDU");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnUpdate(
                        rateSheetId, itemId, "A", "DDU", "B2B", "LV"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void assertImportNotDuplicate_shouldRejectExistingDatabaseItem() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("A");
        existing.setShipmentType("DDU");
        existing.setBusinessType("ALL");
        existing.setCategoryType("HV");
        var item = new RateSheetItemDuplicateValidator.ItemKey("A", "DDU", "ALL", "HV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertImportNotDuplicate(rateSheetId, List.of(item)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resolveItemServiceCode_shouldFallBackToDescriptionForLegacyRows() {
        RateSheetItem legacy = new RateSheetItem();
        legacy.setDescription("LEGACY");

        assertThat(RateSheetItemDuplicateValidator.resolveItemServiceCode(legacy)).isEqualTo("LEGACY");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldRejectWhenCatchAllExists() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("ALL");
        existing.setBusinessType("ALL");
        existing.setCategoryType("ALL");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "DDU", "B2B", "LV"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Shipment Type 'ALL', Business Type 'ALL', and Category Type 'ALL'")
                .hasMessageContaining("No other Shipment Type, Business Type, or Category Type");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldRejectCatchAllWhenSpecificRowsExist() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("DDO");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "ALL", "ALL", "ALL"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot add Service Code 'DUTY'")
                .hasMessageContaining("other rate configurations already exist");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldAllowDifferentShipmentTypesForSameServiceCode() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("DDU");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatCode(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "DDP", "B2B", "LV"))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNoDuplicatesInBatch_shouldRejectCatchAllWithSpecificRows() {
        var catchAll = new RateSheetItemDuplicateValidator.ItemKey("DUTY", "ALL", "ALL", "ALL");
        var specific = new RateSheetItemDuplicateValidator.ItemKey("DUTY", "DDO", "B2B", "LV");

        assertThatThrownBy(() -> validator.assertNoDuplicatesInBatch(List.of(catchAll, specific)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot add Service Code 'DUTY'");
    }

    @Test
    void assertImportNotDuplicate_shouldRejectCatchAllConflictAgainstDatabase() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("ALL");
        existing.setBusinessType("ALL");
        existing.setCategoryType("ALL");
        var item = new RateSheetItemDuplicateValidator.ItemKey("DUTY", "DDP", "B2C", "HV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertImportNotDuplicate(rateSheetId, List.of(item)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Import rejected");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldRejectPartialWildcardOverlap() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("ALL");
        existing.setBusinessType("ALL");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "ALL", "B2C", "LV"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("overlaps with an existing rate configuration");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldRejectWhenIncomingWildcardOverlapsExistingSpecific() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("DDU");
        existing.setBusinessType("B2B");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "ALL", "B2B", "LV"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("overlaps with an existing rate configuration");
    }

    @Test
    void assertNoDuplicatesInBatch_shouldRejectPartialWildcardOverlap() {
        var broad = new RateSheetItemDuplicateValidator.ItemKey("DUTY", "ALL", "ALL", "LV");
        var specific = new RateSheetItemDuplicateValidator.ItemKey("DUTY", "ALL", "B2C", "LV");

        assertThatThrownBy(() -> validator.assertNoDuplicatesInBatch(List.of(broad, specific)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("overlaps with an existing rate configuration");
    }

    @Test
    void assertNotDuplicateOnCreate_shouldAllowNonOverlappingConfigurations() {
        UUID rateSheetId = UUID.randomUUID();
        RateSheetItem existing = new RateSheetItem();
        existing.setServiceCode("DUTY");
        existing.setShipmentType("DDU");
        existing.setBusinessType("ALL");
        existing.setCategoryType("LV");

        when(rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId))
                .thenReturn(List.of(existing));

        assertThatCode(() -> validator.assertNotDuplicateOnCreate(
                        rateSheetId, "DUTY", "DDP", "B2C", "LV"))
                .doesNotThrowAnyException();
    }
}
