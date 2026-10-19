package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.charge.ShipmentChargeDetailsResponse;
import com.customcharge.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardChargeControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardChargeController controller;

    @Test
    void getChargeDetails_shouldDelegateToService() {
        ShipmentChargeDetailsResponse response = ShipmentChargeDetailsResponse.builder()
                .hawbNumber("AWB-1")
                .requestedChargeType("DTC")
                .build();
        when(dashboardService.getShipmentChargeDetails("AWB-1", "DTC")).thenReturn(response);

        ShipmentChargeDetailsResponse result = controller.getChargeDetails("AWB-1", "DTC");

        assertThat(result.getHawbNumber()).isEqualTo("AWB-1");
        assertThat(result.getRequestedChargeType()).isEqualTo("DTC");
    }
}
