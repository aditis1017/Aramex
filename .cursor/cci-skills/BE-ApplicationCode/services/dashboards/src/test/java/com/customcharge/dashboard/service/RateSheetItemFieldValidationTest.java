package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.ServiceCodeDropdownOption;
import com.customcharge.dashboard.service.impl.ServiceChargeService;
import com.customcharge.persistencelayer.cache.MasterCategoryValueLookupService;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetItemFieldValidationTest {

    @Mock
    private MasterCategoryValueLookupService masterCategoryValueLookupService;

    @Mock
    private ServiceChargeService serviceChargeService;

    @Mock
    private RateSheetItemDuplicateValidator duplicateValidator;

    private RateSheetItemFieldValidation validation;

    @BeforeEach
    void setUp() {
        validation = new RateSheetItemFieldValidation(
                masterCategoryValueLookupService, serviceChargeService, duplicateValidator);
        lenient().when(serviceChargeService.getActiveServiceCodesForDropdown(
                        anyString(), anyString(), anyString()))
                .thenReturn(List.of(new ServiceCodeDropdownOption("SC01", "Service One")));
    }

    @Test
    void normalizeFields_shouldValidateServiceCodeAndShipmentType() {
        RateSheetItemFieldValidation.ValidatedFields fields =
                validation.normalizeFields(
                        "account", "ENT", "ECD", " SC01 ", " DDU ", " ALL ", " LV ", null);

        assertThat(fields.serviceCode()).isEqualTo("SC01");
        assertThat(fields.shipmentType()).isEqualTo("DDU");
        assertThat(fields.businessType()).isEqualTo("ALL");
        assertThat(fields.categoryType()).isEqualTo("LV");
        verify(masterCategoryValueLookupService).requireActiveCodeInCategory(
                eq("CUSTOMS_TYPE"), eq("DDU"), anyString());
    }

    @Test
    void normalizeFields_shouldAcceptAllShipmentTypeWithoutMasterLookup() {
        RateSheetItemFieldValidation.ValidatedFields fields =
                validation.normalizeFields(
                        "account", "ENT", "ECD", " SC01 ", " all ", " B2B ", " LV ", null);

        assertThat(fields.shipmentType()).isEqualTo("ALL");
        verify(masterCategoryValueLookupService, never()).requireActiveCodeInCategory(
                eq("CUSTOMS_TYPE"), eq("ALL"), anyString());
    }

    @Test
    void normalizeFields_shouldRejectBlankServiceCode() {
        assertThatThrownBy(() -> validation.normalizeFields(
                        "account", "ENT", "ECD", " ", "DDU", "B2B", "LV", 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Row 1");
    }

    @Test
    void normalizeFields_shouldRejectInvalidServiceCode() {
        assertThatThrownBy(() -> validation.normalizeFields(
                        "account", "ENT", "ECD", "BAD", "DDU", "B2B", "LV", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Row 2")
                .hasMessageContaining("not valid");
    }

    @Test
    void normalizeFields_shouldRejectBlankBusinessType() {
        assertThatThrownBy(() -> validation.normalizeFields(
                        "account", "ENT", "ECD", "SC01", "DDU", " ", "LV", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Business Type is required");
    }

    @Test
    void normalizeFields_shouldNormalizeAllCategoryType() {
        RateSheetItemFieldValidation.ValidatedFields fields =
                validation.normalizeFields(
                        "account", "ENT", "ECD", " SC01 ", " DDU ", " B2B ", " all ", null);

        assertThat(fields.categoryType()).isEqualTo("ALL");
    }

    @Test
    void validateForCreate_shouldNormalizeAndCheckDuplicates() {
        UUID rateSheetId = UUID.randomUUID();

        RateSheetItemFieldValidation.ValidatedFields fields =
                validation.validateForCreate(
                        rateSheetId, "account", "ENT", "ECD", " SC01 ", " DDU ", " B2B ", " HV ");

        assertThat(fields.businessType()).isEqualTo("B2B");
        assertThat(fields.categoryType()).isEqualTo("HV");
        verify(duplicateValidator).assertNotDuplicateOnCreate(
                eq(rateSheetId), eq("SC01"), eq("DDU"), eq("B2B"), eq("HV"));
    }

    @Test
    void validateForCreate_shouldRejectInvalidShipmentType() {
        UUID rateSheetId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("invalid"))
                .when(masterCategoryValueLookupService)
                .requireActiveCodeInCategory(eq("CUSTOMS_TYPE"), eq("BAD"), anyString());

        assertThatThrownBy(() -> validation.validateForCreate(
                        rateSheetId, "account", "ENT", "ECD", "SC01", "BAD", "B2B", "LV"))
                .isInstanceOf(BadRequestException.class);
    }
}
