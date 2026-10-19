package com.customcharge.persistencelayer.calculatorhelper;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Aggregates {@code Charge} header amounts from line items using charge-type rules.
 * <p>
 * X = contractedRate null/zero ? amount : contractedRate
 */
public final class ChargeHeaderAmountSupport {

    public static final String ECD = "ECD";
    public static final String ECS = "ECS";
    public static final String EC3 = "EC3";
    public static final String DTC = "DTC";
    public static final String DTA = "DTA";
    public static final String DTS = "DTS";
    public static final String DT3 = "DT3";

    private static final Set<String> COST_CHARGE_CODES = Set.of(ECD, ECS, EC3);
    private static final Set<String> REVENUE_CHARGE_CODES = Set.of(DTC, DTA, DTS, DT3);
    private static final Set<String> ECS_EC3_CHARGE_CODES = Set.of(ECS, EC3);

    private ChargeHeaderAmountSupport() {
    }

    public record ChargeHeaderAmounts(
            BigDecimal estimateAmount,
            BigDecimal actualAmount,
            BigDecimal outstandingAmount) {

        public static ChargeHeaderAmounts zero() {
            return new ChargeHeaderAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public static ChargeHeaderAmounts compute(String chargeCode, List<ChargeLineItem> lines) {
        String normalized = normalize(chargeCode);
        if (lines == null || lines.isEmpty()) {
            return ChargeHeaderAmounts.zero();
        }
        if (COST_CHARGE_CODES.contains(normalized)) {
            return computeCostHeaderAmounts(normalized, lines);
        }
        if (REVENUE_CHARGE_CODES.contains(normalized)) {
            return computeRevenueHeaderAmounts(lines);
        }
        return ChargeHeaderAmounts.zero();
    }

    public static void applyToCharge(
            Charge charge,
            List<ChargeLineItem> lines,
            ChargeHeaderAmountFormatter formatter) {
        if (charge == null || formatter == null) {
            return;
        }
        ChargeHeaderAmounts amounts = compute(charge.getChargeCode(), lines);
        charge.setEstimateAmount(formatter.format(amounts.estimateAmount()));
        charge.setActualAmount(formatter.format(amounts.actualAmount()));
        charge.setOutstandingAmount(formatter.format(amounts.outstandingAmount()));
    }

    public interface ChargeHeaderAmountFormatter {
        BigDecimal format(BigDecimal amount);
    }

    private static ChargeHeaderAmounts computeCostHeaderAmounts(String chargeCode, List<ChargeLineItem> lines) {
        BigDecimal estimateAmount = sumLineAmounts(lines, shouldExcludePaymentRefLinesFromEstimate(chargeCode));
        BigDecimal actualAmount = ECD.equals(chargeCode)
                ? sumLineAmountsWherePaymentReferencePresent(lines)
                : BigDecimal.ZERO;
        return new ChargeHeaderAmounts(estimateAmount, actualAmount, BigDecimal.ZERO);
    }

    private static ChargeHeaderAmounts computeRevenueHeaderAmounts(List<ChargeLineItem> lines) {
        BigDecimal sumX = sumEffectiveX(lines);
        return new ChargeHeaderAmounts(sumX, BigDecimal.ZERO, sumX);
    }

    private static boolean shouldExcludePaymentRefLinesFromEstimate(String chargeCode) {
        return ECS_EC3_CHARGE_CODES.contains(chargeCode);
    }

    private static BigDecimal sumLineAmounts(List<ChargeLineItem> lines, boolean excludePaymentRefLines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChargeLineItem line : lines) {
            if (line == null) {
                continue;
            }
            if (excludePaymentRefLines && hasPaymentReference(line)) {
                continue;
            }
            total = total.add(lineAmount(line));
        }
        return total;
    }

    private static BigDecimal sumLineAmountsWherePaymentReferencePresent(List<ChargeLineItem> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChargeLineItem line : lines) {
            if (line == null || !hasPaymentReference(line)) {
                continue;
            }
            total = total.add(lineAmount(line));
        }
        return total;
    }

    private static BigDecimal sumEffectiveX(List<ChargeLineItem> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChargeLineItem line : lines) {
            if (line == null) {
                continue;
            }
            total = total.add(ChargeLineCopyService.effectiveAmount(line));
        }
        return total;
    }

    private static BigDecimal lineAmount(ChargeLineItem line) {
        return line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
    }

    static boolean hasPaymentReference(ChargeLineItem line) {
        return line.getPaymentReference() != null && !line.getPaymentReference().isBlank();
    }

    private static String normalize(String chargeCode) {
        return chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
    }
}
