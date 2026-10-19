package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShipmentTypeChargeCalculatorRouterTest {

    private ChargeCalculation ecd;
    private ChargeCalculation dtc;
    private ChargeCalculation dta;
    private ChargeCalculation ecs;
    private ChargeCalculation ec3;
    private ChargeCalculation dts;
    private ChargeCalculation dt3;
    private ShipmentTypeChargeCalculatorRouter router;

    @BeforeEach
    void setUp() {
        ecd = mockCalculator("ECD");
        dtc = mockCalculator("DTC");
        dta = mockCalculator("DTA");
        ecs = mockCalculator("ECS");
        ec3 = mockCalculator("EC3");
        dts = mockCalculator("DTS");
        dt3 = mockCalculator("DT3");
        router = new ShipmentTypeChargeCalculatorRouter(List.of(ecd, dtc, dta, ecs, ec3, dts, dt3));
    }

    @Test
    void getCalculators_shouldResolveEstimateDduCalculators() {
        List<ChargeCalculation> calculators = router.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                BatchProcessingConstants.SHIPMENT_TYPE_DDU);

        assertThat(calculators).containsExactly(ecd, dtc);
    }

    @Test
    void getCalculators_shouldResolveEstimateDdpCalculators() {
        List<ChargeCalculation> calculators = router.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                BatchProcessingConstants.SHIPMENT_TYPE_DDP);

        assertThat(calculators).containsExactly(ecd, dta, ecs, ec3, dts, dt3);
    }

    @Test
    void getCalculators_shouldResolveActualDduCalculators() {
        List<ChargeCalculation> calculators = router.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                BatchProcessingConstants.SHIPMENT_TYPE_DDU);

        assertThat(calculators).containsExactly(ecd);
    }

    @Test
    void getCalculators_shouldResolveActualDdoCalculators() {
        List<ChargeCalculation> calculators = router.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                BatchProcessingConstants.SHIPMENT_TYPE_DDO);

        assertThat(calculators).containsExactly(ecd);
    }

    @Test
    void getCalculators_shouldResolveActualDdpCalculators() {
        List<ChargeCalculation> calculators = router.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                BatchProcessingConstants.SHIPMENT_TYPE_DDP);

        assertThat(calculators).containsExactly(ecd);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "Unknown"})
    void getCalculators_shouldRejectUnsupportedProcessFor(String processFor) {
        String shipmentType = BatchProcessingConstants.SHIPMENT_TYPE_DDU;
        assertThatThrownBy(() -> router.getCalculators(processFor, shipmentType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(BatchProcessingConstants.ERROR_UNSUPPORTED_PROCESS_FOR);
    }

    @ParameterizedTest
    @MethodSource("invalidShipmentTypeCases")
    void getCalculators_shouldRejectInvalidShipmentType(String shipmentType, String expectedMessageFragment) {
        String processFor = BatchProcessingConstants.PROCESS_FOR_ESTIMATE;
        assertThatThrownBy(() -> router.getCalculators(processFor, shipmentType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(expectedMessageFragment);
    }

    private static Stream<Arguments> invalidShipmentTypeCases() {
        return Stream.of(
                Arguments.of(null, BatchProcessingConstants.ERROR_MISSING_SHIPMENT_TYPE),
                Arguments.of("FOB", BatchProcessingConstants.ERROR_UNSUPPORTED_SHIPMENT_TYPE));
    }

    private static ChargeCalculation mockCalculator(String code) {
        ChargeCalculation calculator = org.mockito.Mockito.mock(ChargeCalculation.class);
        org.mockito.Mockito.when(calculator.getChargeCode()).thenReturn(code);
        return calculator;
    }
}
