package com.customcharge.shipment.controller;

import com.customcharge.shipment.dto.ShipmentListenerMatrixResponse;
import com.customcharge.shipment.service.ShipmentListenerMatrixService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentListenerMatrixControllerTest {

    @Mock
    private ShipmentListenerMatrixService shipmentListenerMatrixService;

    @InjectMocks
    private ShipmentListenerMatrixController controller;

    @Test
    void listenerMatrix_shouldReturnCapturedMatrix() {
        ShipmentListenerMatrixResponse matrix = ShipmentListenerMatrixResponse.builder()
                .capturedAt(Instant.now())
                .instanceId("pod-1")
                .queue("HB_CCI_MSG5")
                .listeners(List.of())
                .build();
        when(shipmentListenerMatrixService.captureMatrix()).thenReturn(matrix);

        ShipmentListenerMatrixResponse response = controller.listenerMatrix();

        assertThat(response.getInstanceId()).isEqualTo("pod-1");
    }
}
