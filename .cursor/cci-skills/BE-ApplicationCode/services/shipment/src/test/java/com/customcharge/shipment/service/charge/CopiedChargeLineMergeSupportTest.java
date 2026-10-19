package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CopiedChargeLineMergeSupportTest {

    @Test
    void merge_shouldReturnRevenueLines_whenCopiedEmpty() {
        ChargeLineItem revenue = line("REV_FEE", "25");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(), List.of(revenue));

        assertThat(merged).containsExactly(revenue);
    }

    @Test
    void merge_shouldReturnCopiedLines_whenRevenueEmpty() {
        ChargeLineItem copied = line("DUTY", "50");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copied), List.of());

        assertThat(merged).containsExactly(copied);
    }

    @Test
    void merge_shouldOverrideCopiedCost_whenSameServiceCodeOnRevenue() {
        ChargeLineItem copiedDuty = line("DUTY", "50");
        ChargeLineItem copiedVat = line("VAT", "150");
        ChargeLineItem copiedOf = line("OF", "10");
        ChargeLineItem revenueDuty = line("DUTY", "55");
        ChargeLineItem revenueVat = line("VAT", "160");
        ChargeLineItem revenueFee = line("REV_FEE", "25");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copiedDuty, copiedVat, copiedOf),
                List.of(revenueDuty, revenueVat, revenueFee));

        assertThat(merged).extracting(ChargeLineItem::getServiceCode)
                .containsExactly("DUTY", "VAT", "OF", "REV_FEE");
        assertThat(findAmount(merged, "DUTY")).isEqualByComparingTo("55");
        assertThat(findAmount(merged, "VAT")).isEqualByComparingTo("160");
        assertThat(findAmount(merged, "OF")).isEqualByComparingTo("10");
        assertThat(findAmount(merged, "REV_FEE")).isEqualByComparingTo("25");
    }

    @Test
    void merge_shouldApplyForDtsFromEcsPair() {
        ChargeLineItem copied = line("ORIGIN_DUTY", "40");
        ChargeLineItem revenue = line("ORIGIN_DUTY", "45");
        ChargeLineItem revenueOnly = line("DTS_REV", "5");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copied), List.of(revenue, revenueOnly));

        assertThat(merged).hasSize(2);
        assertThat(findAmount(merged, "ORIGIN_DUTY")).isEqualByComparingTo("45");
    }

    @Test
    void merge_shouldApplyForDt3FromEc3Pair() {
        ChargeLineItem copied = line("TP_COST", "30");
        ChargeLineItem revenue = line("TP_REV", "8");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copied), List.of(revenue));

        assertThat(merged).extracting(ChargeLineItem::getServiceCode)
                .containsExactly("TP_COST", "TP_REV");
    }

    @Test
    void merge_shouldNormalizeServiceCode_whenComparing() {
        ChargeLineItem copied = line("duty", "50");
        ChargeLineItem revenue = line(" DUTY ", "55");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copied), List.of(revenue));

        assertThat(merged).hasSize(1);
        assertThat(merged.getFirst().getAmount()).isEqualByComparingTo("55");
    }

    @Test
    void merge_shouldPreserveLinesWithoutServiceCode() {
        ChargeLineItem copiedNoCode = line(null, "1");
        ChargeLineItem copiedDuty = line("DUTY", "50");
        ChargeLineItem revenueNoCode = line(null, "2");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(copiedNoCode, copiedDuty), List.of(revenueNoCode));

        assertThat(merged).hasSize(3);
        assertThat(merged.get(0)).isSameAs(copiedNoCode);
        assertThat(merged.get(2)).isSameAs(revenueNoCode);
    }

    @Test
    void merge_shouldDeduplicateDuplicateCopiedServiceCodes() {
        ChargeLineItem first = line("DUTY", "50");
        ChargeLineItem duplicate = line("DUTY", "60");
        ChargeLineItem revenue = line("REV_FEE", "25");

        List<ChargeLineItem> merged = CopiedChargeLineMergeSupport.mergeCopiedWithRevenueLines(
                List.of(first, duplicate), List.of(revenue));

        assertThat(merged).extracting(ChargeLineItem::getServiceCode)
                .containsExactly("DUTY", "REV_FEE");
        assertThat(findAmount(merged, "DUTY")).isEqualByComparingTo("50");
    }

    private static ChargeLineItem line(String serviceCode, String amount) {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode(serviceCode);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static BigDecimal findAmount(List<ChargeLineItem> lines, String serviceCode) {
        return lines.stream()
                .filter(line -> serviceCode.equalsIgnoreCase(line.getServiceCode()))
                .map(ChargeLineItem::getAmount)
                .findFirst()
                .orElseThrow();
    }
}
