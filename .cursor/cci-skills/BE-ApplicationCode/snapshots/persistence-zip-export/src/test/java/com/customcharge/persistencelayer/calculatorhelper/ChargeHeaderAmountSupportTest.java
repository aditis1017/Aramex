package com.customcharge.persistencelayer.calculatorhelper;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeHeaderAmountSupportTest {

    @Test
    void compute_ecd_shouldSumAllAmountsForEstimateAndPaymentRefForActual() {
        ChargeLineItem estimate = line("10", null, null);
        ChargeLineItem actual = line("7", "PAY-1", null);

        ChargeHeaderAmountSupport.ChargeHeaderAmounts amounts =
                ChargeHeaderAmountSupport.compute(ChargeHeaderAmountSupport.ECD, List.of(estimate, actual));

        assertThat(amounts.estimateAmount()).isEqualByComparingTo("17");
        assertThat(amounts.actualAmount()).isEqualByComparingTo("7");
        assertThat(amounts.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void compute_ecs_shouldExcludePaymentRefLinesFromEstimate() {
        ChargeLineItem estimate = line("12", null, null);
        ChargeLineItem actual = line("5", "PAY-1", null);

        ChargeHeaderAmountSupport.ChargeHeaderAmounts amounts =
                ChargeHeaderAmountSupport.compute(ChargeHeaderAmountSupport.ECS, List.of(estimate, actual));

        assertThat(amounts.estimateAmount()).isEqualByComparingTo("12");
        assertThat(amounts.actualAmount()).isEqualByComparingTo("0");
        assertThat(amounts.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void compute_ec3_shouldExcludePaymentRefLinesFromEstimate() {
        ChargeLineItem estimate = line("8", null, null);
        ChargeLineItem actual = line("3", "PAY-1", null);

        ChargeHeaderAmountSupport.ChargeHeaderAmounts amounts =
                ChargeHeaderAmountSupport.compute(ChargeHeaderAmountSupport.EC3, List.of(estimate, actual));

        assertThat(amounts.estimateAmount()).isEqualByComparingTo("8");
        assertThat(amounts.actualAmount()).isEqualByComparingTo("0");
        assertThat(amounts.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void compute_revenue_shouldUseEffectiveXForEstimateAndOutstanding() {
        ChargeLineItem amountOnly = line("100", null, null);
        ChargeLineItem contracted = line("80", null, "50");

        ChargeHeaderAmountSupport.ChargeHeaderAmounts amounts =
                ChargeHeaderAmountSupport.compute(ChargeHeaderAmountSupport.DTC, List.of(amountOnly, contracted));

        assertThat(amounts.estimateAmount()).isEqualByComparingTo("150");
        assertThat(amounts.actualAmount()).isEqualByComparingTo("0");
        assertThat(amounts.outstandingAmount()).isEqualByComparingTo("150");
    }

    @Test
    void compute_emptyLines_shouldReturnZero() {
        ChargeHeaderAmountSupport.ChargeHeaderAmounts amounts =
                ChargeHeaderAmountSupport.compute(ChargeHeaderAmountSupport.DTA, List.of());

        assertThat(amounts.estimateAmount()).isEqualByComparingTo("0");
        assertThat(amounts.actualAmount()).isEqualByComparingTo("0");
        assertThat(amounts.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void applyToCharge_shouldSetAllHeaderFields() {
        com.customcharge.persistencelayer.entities.Charge charge = new com.customcharge.persistencelayer.entities.Charge();
        charge.setChargeCode(ChargeHeaderAmountSupport.DTC);
        ChargeLineItem line = line("100", null, "40");

        ChargeHeaderAmountSupport.applyToCharge(
                charge,
                List.of(line),
                amount -> amount);

        assertThat(charge.getEstimateAmount()).isEqualByComparingTo("40");
        assertThat(charge.getActualAmount()).isEqualByComparingTo("0");
        assertThat(charge.getOutstandingAmount()).isEqualByComparingTo("40");
    }

    private static ChargeLineItem line(String amount, String paymentReference, String contractedRate) {
        ChargeLineItem line = new ChargeLineItem();
        line.setAmount(new BigDecimal(amount));
        line.setPaymentReference(paymentReference);
        if (contractedRate != null) {
            line.setContractedRate(new BigDecimal(contractedRate));
        }
        return line;
    }
}
