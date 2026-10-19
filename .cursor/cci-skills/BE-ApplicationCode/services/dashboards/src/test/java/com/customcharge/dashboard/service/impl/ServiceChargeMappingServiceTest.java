package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceChargeMappingServiceTest {

    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;

    @Mock
    private ServiceCodesRepository serviceCodesRepository;

    @Mock
    private ServiceCodesService serviceCodesService;

    @InjectMocks
    private ServiceChargeMappingService service;

    @Test
    void getMappingScreenData_shouldRejectInvalidDestinationChargeType() {
        assertThatThrownBy(() -> service.getMappingScreenData("DTC", "DXB", "RUH", false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getMappingScreenData_shouldRejectExistingCombination() {
        when(serviceChargeMappingRepository.existsByFromEntityAndToEntityAndChargeType("DXB", "RUH", "ECS"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.getMappingScreenData("ECS", "DXB", "RUH", false))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getGroupedServiceCharges_shouldReturnEmptyWhenNoMappings() {
        when(serviceChargeMappingRepository.findByToEntityAndChargeType("RUH", ChargeTypeConstants.ECS))
                .thenReturn(java.util.List.of());

        var result = service.getGroupedServiceCharges("RUH", "ECS");

        org.assertj.core.api.Assertions.assertThat(result).isEmpty();
    }
}
